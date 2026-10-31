package com.mulkora.feature.feed.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostStatus
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.feature.feed.data.dto.PostDto
import com.mulkora.feature.feed.data.paging.FeedPagingSource
import com.mulkora.feature.feed.domain.model.FeedItem
import com.mulkora.feature.feed.domain.model.Post
import com.mulkora.feature.feed.domain.model.ReactionType
import com.mulkora.feature.feed.domain.repository.FeedRepository
import com.mulkora.feature.feed.domain.repository.FeedSortOption
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feed Repository gerçeklemesi.
 * CLAUDE.md §7, §10: Gönderi okuma, tepki, puanlama.
 * CLAUDE.md §26: Sayaçlar istemciden değiştirilemez → CF kullanılır.
 */
@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : FeedRepository {

    companion object {
        private const val PAGE_SIZE = 20
        private const val POSTS_COLLECTION = "posts"
        private const val USERS_COLLECTION = "users"
    }

    // ─── Yardımcı: suspend işlemleri için güvenli sarmalayıcı ────────────────
    // Kotlin stdlib runCatching yerine proje Result<T> tipini döndürür.
    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }

    // ─── Sayfalı akış ────────────────────────────────────────────────────────

    override fun getApprovedPostsPaged(
        category: PostCategory?,
        postType: PostType?,
        sortBy: FeedSortOption,
    ): Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = 5,
        ),
        pagingSourceFactory = {
            FeedPagingSource(
                firestore = firestore,
                sortOption = sortBy,
                categoryFilter = category?.name?.lowercase(),
                typeFilter = postType?.name?.lowercase(),
            )
        },
    ).flow.map { pagingData ->
        pagingData.map { post -> FeedItem.PostItem(post) }
    }

    // ─── Tek gönderi (realtime) ───────────────────────────────────────────────

    override fun getPostById(postId: String): Flow<Result<Post?>> = callbackFlow {
        val listener = firestore
            .collection(POSTS_COLLECTION)
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val post = snapshot?.toObject(PostDto::class.java)?.toPost()
                trySend(Result.Success(post))
            }
        awaitClose { listener.remove() }
    }

    // ─── Kullanıcı gönderileri ────────────────────────────────────────────────

    override fun getPostsByUser(uid: String): Flow<Result<List<Post>>> = callbackFlow {
        val listener = firestore
            .collection(POSTS_COLLECTION)
            .whereEqualTo("authorUid", uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PostDto::class.java)?.toPost() }
                    ?: emptyList()
                trySend(Result.Success(posts))
            }
        awaitClose { listener.remove() }
    }

    // ─── Gönderi CRUD ─────────────────────────────────────────────────────────

    override suspend fun createPost(
        title: String,
        content: String,
        postType: PostType,
        category: PostCategory,
        tags: List<String>,
        imageUri: String?,
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

        return try {
            val userSnap = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val userData = userSnap.data
                ?: return Result.Error(IllegalStateException("Kullanıcı verisi bulunamadı."))

            val postRef = firestore.collection(POSTS_COLLECTION).document()
            val now = FieldValue.serverTimestamp()

            // imageUri: Storage URL (Storage'a yükleme ImageUploadRepository tarafından yapılır;
            // bu noktaya geldiğinde imageUri zaten bir Firebase Storage download URL'sidir)
            val postData = hashMapOf<String, Any?>(
                "id" to postRef.id,
                "authorUid" to uid,
                "authorUsername" to (userData["username"] as? String ?: ""),
                "authorDisplayName" to (userData["displayName"] as? String ?: ""),
                "authorPhotoUrl" to (userData["photoUrl"] as? String),
                "authorRole" to (userData["role"] as? String ?: "user"),
                "title" to title,
                "content" to content,
                "postType" to postType.name.lowercase(),
                "category" to category.name.lowercase(),
                "tags" to tags,
                // imageUrl: tekil alan — PostDto ile eşleşir (CLAUDE.md §15: her gönderiye en fazla bir görsel)
                "imageUrl" to imageUri,
                "status" to PostStatus.DRAFT.name.lowercase(),
                "likeCount" to 0L,
                "dislikeCount" to 0L,
                "viewCount" to 0L,
                "commentCount" to 0L,
                "averageRating" to 0.0,
                "ratingCount" to 0L,
                "isFlagged" to false,
                "createdAt" to now,
                "updatedAt" to now,
            )

            postRef.set(postData).await()

            // Taslağı hemen moderasyon kuyruğuna gönder (draft → pending)
            // CLAUDE.md §8: Tüm yeni gönderiler yönetici/moderatör onayından geçmeli.
            functions
                .getHttpsCallable("submitPost")
                .call(mapOf("postId" to postRef.id))
                .await()

            Result.Success(postRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePost(
        postId: String,
        title: String,
        content: String,
        postType: PostType,
        category: PostCategory,
        tags: List<String>,
        imageUri: String?,
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "title" to title,
                "content" to content,
                "postType" to postType.name.lowercase(),
                "category" to category.name.lowercase(),
                "tags" to tags,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (imageUri != null) {
                updates["imageUrls"] = listOf(imageUri)
            }
            firestore.collection(POSTS_COLLECTION).document(postId)
                .update(updates)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION).document(postId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Tepki (beğeni/eksi oy) ───────────────────────────────────────────────

    /**
     * CLAUDE.md §10: Tepkiler Cloud Functions üzerinden işlenir.
     * Sayaçlar transaction ile atomik olarak güncellenir.
     */
    override suspend fun toggleReaction(
        postId: String,
        reactionType: ReactionType,
    ): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("togglePostReaction")
                .call(
                    mapOf(
                        "postId" to postId,
                        "reactionType" to reactionType.value,
                    )
                )
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Puanlama ─────────────────────────────────────────────────────────────

    /**
     * CLAUDE.md §10: Puanlama Cloud Functions üzerinden yapılır.
     * Ortalama puan transaction ile atomik güncellenir.
     */
    override suspend fun ratePost(postId: String, rating: Int): Result<Unit> {
        if (rating !in 1..5) {
            return Result.Error(IllegalArgumentException("Puan 1–5 arasında olmalıdır."))
        }
        return try {
            functions
                .getHttpsCallable("ratePost")
                .call(
                    mapOf(
                        "postId" to postId,
                        "rating" to rating,
                    )
                )
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Kaydetme ─────────────────────────────────────────────────────────────

    override suspend fun toggleSavePost(postId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(uid)
            firestore.runTransaction { transaction ->
                val snap = transaction.get(userRef)
                @Suppress("UNCHECKED_CAST")
                val saved = snap.get("savedPostIds") as? List<String> ?: emptyList()
                if (saved.contains(postId)) {
                    transaction.update(userRef, "savedPostIds", FieldValue.arrayRemove(postId))
                } else {
                    transaction.update(userRef, "savedPostIds", FieldValue.arrayUnion(postId))
                }
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getSavedPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.Error(IllegalStateException("Kullanıcı oturumu yok.")))
            close()
            return@callbackFlow
        }

        val listener = firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .addSnapshotListener { userSnap, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                val savedIds = userSnap?.get("savedPostIds") as? List<String> ?: emptyList()

                if (savedIds.isEmpty()) {
                    trySend(Result.Success(emptyList()))
                    return@addSnapshotListener
                }

                // Firestore `in` sorgusu max 30 öğe destekler
                val chunks = savedIds.chunked(30)
                val allPosts = mutableListOf<Post>()

                chunks.forEach { chunk ->
                    firestore.collection(POSTS_COLLECTION)
                        .whereIn("id", chunk)
                        .get()
                        .addOnSuccessListener { snap ->
                            val posts = snap.documents
                                .mapNotNull { it.toObject(PostDto::class.java)?.toPost() }
                            allPosts.addAll(posts)
                            trySend(Result.Success(allPosts.toList()))
                        }
                        .addOnFailureListener { e ->
                            trySend(Result.Error(e))
                        }
                }
            }
        awaitClose { listener.remove() }
    }

    // ─── Gönderi gönderme ─────────────────────────────────────────────────────

    /**
     * Taslağı incelemeye gönderir (draft → pending).
     * submitPost Cloud Function çağrılır.
     * CLAUDE.md §8: Tüm gönderiler moderasyon kuyruğuna alınır.
     */
    override suspend fun submitPost(postId: String): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("submitPost")
                .call(mapOf("postId" to postId))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Görüntülenme sayacı ──────────────────────────────────────────────────

    /**
     * Görüntülenme sayacını Cloud Function üzerinden artırır.
     * CLAUDE.md §26: Sayaçlar istemciden değiştirilemez — CF kullanılır.
     * İzleme amaçlı, hata kritik değil.
     */
    suspend fun incrementViewCount(postId: String) {
        try {
            functions
                .getHttpsCallable("incrementViewCount")
                .call(mapOf("postId" to postId))
                .await()
        } catch (_: Exception) {
            // Sessizce görmezden gel — kritik değil
        }
    }
}