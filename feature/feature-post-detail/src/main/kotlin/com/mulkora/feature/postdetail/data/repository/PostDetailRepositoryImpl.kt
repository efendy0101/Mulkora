package com.mulkora.feature.postdetail.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostStatus
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.model.UserRole
import com.mulkora.core.common.result.Result
import com.mulkora.core.firebase.util.FirebaseCollections
import com.mulkora.feature.postdetail.domain.model.PostDetail
import com.mulkora.feature.postdetail.domain.repository.PostDetailRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * PostDetailRepository implementasyonu.
 *
 * CLAUDE.md §9 + §33:
 * - Firestore okuma işlemleri (observePostDetail) bu sınıftan gerçekleşir.
 * - SAYAÇ DEĞİŞTİREN işlemler (tepki, puan) YALNIZCA Cloud Functions üzerinden yapılır.
 *   İstemci doğrudan postVotes/postRatings alt koleksiyonlarına kesinlikle yazamaz.
 *
 * CF isimleri (functions/src/index.ts ile uyumlu):
 *   togglePostReaction — reactionType: "like" | "dislike"
 *   ratePost           — rating: 1–5
 */
class PostDetailRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) : PostDetailRepository {

    override fun observePostDetail(postId: String): Flow<Result<PostDetail>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        val postRef = firestore.collection(FirebaseCollections.POSTS).document(postId)

        // Kullanıcı reaksiyonu: posts/{postId}/reactions/{uid} (togglePostReaction CF yazar)
        val reactionRef = if (currentUserId != null)
            firestore.collection(FirebaseCollections.POSTS).document(postId)
                .collection("reactions").document(currentUserId)
        else null

        // Kullanıcı puanı: postRatings/{postId}_{uid} flat belge (ratePost CF yazar)
        val ratingRef = if (currentUserId != null)
            firestore.collection("postRatings").document("${postId}_${currentUserId}")
        else null

        // Kullanıcının bu gönderiyi kaydetmiş olup olmadığı: users/{uid}/savedPosts/{postId}
        val savedRef = if (currentUserId != null)
            firestore.collection(FirebaseCollections.USERS)
                .document(currentUserId)
                .collection("savedPosts")
                .document(postId)
        else null

        var postData: Map<String, Any>? = null
        var reactionType: String? = null   // "like" | "dislike" | null
        var userRating: Int? = null
        var isSaved: Boolean = false

        fun emitCurrent() {
            val data = postData ?: return
            val currentUserVote = when (reactionType) {
                "like" -> 1
                "dislike" -> -1
                else -> 0
            }
            val postDetail = PostDetail(
                id = postId,
                authorId = data["authorId"] as? String ?: data["authorUid"] as? String ?: "",
                authorUsername = data["authorUsername"] as? String ?: "",
                authorDisplayName = data["authorDisplayName"] as? String ?: "",
                authorPhotoUrl = data["authorPhotoUrl"] as? String,
                authorRole = runCatching {
                    UserRole.valueOf(data["authorRole"] as? String ?: "TENANT")
                }.getOrDefault(UserRole.TENANT),
                title = data["title"] as? String ?: "",
                body = data["body"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String,
                category = runCatching {
                    PostCategory.fromValue(data["category"] as? String ?: "general")
                }.getOrDefault(PostCategory.GENERAL),
                type = runCatching {
                    PostType.fromValue(data["type"] as? String ?: "info")
                }.getOrDefault(PostType.INFO),
                city = data["city"] as? String ?: "",
                isAnonymous = data["isAnonymous"] as? Boolean ?: false,
                commentsAllowed = data["commentsAllowed"] as? Boolean ?: true,
                status = runCatching {
                    PostStatus.valueOf(data["status"] as? String ?: "APPROVED")
                }.getOrDefault(PostStatus.APPROVED),
                likeCount = (data["likeCount"] as? Long)?.toInt() ?: 0,
                dislikeCount = (data["dislikeCount"] as? Long)?.toInt() ?: 0,
                commentCount = (data["commentCount"] as? Long)?.toInt() ?: 0,
                averageRating = when (val v = data["averageRating"]) {
                    is Double -> v
                    is Long -> v.toDouble()
                    else -> 0.0
                },
                ratingCount = (data["ratingCount"] as? Long)?.toInt() ?: 0,
                currentUserVote = currentUserVote,
                currentUserRating = userRating,
                isSaved = isSaved,
                createdAt = (data["createdAt"] as? Long) ?: 0L,
                updatedAt = (data["updatedAt"] as? Long) ?: 0L,
            )
            trySend(Result.Success(postDetail))
        }

        val postListener = postRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(error))
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                trySend(Result.Error(NoSuchElementException("Gönderi bulunamadı.")))
                return@addSnapshotListener
            }
            postData = snapshot.data
            emitCurrent()
        }

        val reactionListener = reactionRef?.addSnapshotListener { snapshot, _ ->
            reactionType = snapshot?.getString("reactionType")
            emitCurrent()
        }

        val ratingListener = ratingRef?.addSnapshotListener { snapshot, _ ->
            userRating = (snapshot?.getLong("rating"))?.toInt()
            emitCurrent()
        }

        val savedListener = savedRef?.addSnapshotListener { snapshot, _ ->
            isSaved = snapshot?.exists() == true
            emitCurrent()
        }

        awaitClose {
            postListener.remove()
            reactionListener?.remove()
            ratingListener?.remove()
            savedListener?.remove()
        }
    }

    override suspend fun toggleSavePost(postId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.Error(IllegalStateException("Kullanıcı oturum açmamış."))
            val savedRef = firestore
                .collection(FirebaseCollections.USERS)
                .document(userId)
                .collection("savedPosts")
                .document(postId)
            val doc = savedRef.get().await()
            if (doc.exists()) {
                savedRef.delete().await()
            } else {
                savedRef.set(mapOf("savedAt" to System.currentTimeMillis())).await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Beğeni veya eksi oy tepkisi ekler/kaldırır.
     *
     * CLAUDE.md §9: Sayaç değiştiren işlemler YALNIZCA Cloud Functions üzerinden.
     * togglePostReaction CF çağrılır — reactionType: "like" | "dislike" (lowercase)
     *
     * vote: 1 = beğeni ("like"), -1 = eksi oy ("dislike")
     */
    override suspend fun toggleVote(postId: String, vote: Int): Result<Unit> {
        return try {
            val reactionType = when (vote) {
                1 -> "like"
                -1 -> "dislike"
                else -> return Result.Error(IllegalArgumentException("Geçersiz oy değeri: $vote"))
            }
            functions
                .getHttpsCallable("togglePostReaction")
                .call(mapOf("postId" to postId, "reactionType" to reactionType))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Gönderiye 1-5 yıldız puanı verir.
     *
     * CLAUDE.md §9: ratePost Cloud Function çağrılır — ortalama transaction ile atomik güncellenir.
     */
    override suspend fun ratePost(postId: String, rating: Int): Result<Unit> {
        return try {
            require(rating in 1..5) { "Puan 1-5 arasında olmalıdır." }
            functions
                .getHttpsCallable("ratePost")
                .call(mapOf("postId" to postId, "rating" to rating))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}