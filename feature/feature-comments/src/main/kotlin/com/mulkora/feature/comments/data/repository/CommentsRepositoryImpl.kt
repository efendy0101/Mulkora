package com.mulkora.feature.comments.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.model.UserRole
import com.mulkora.core.common.result.Result
import com.mulkora.feature.comments.domain.model.Comment
import com.mulkora.feature.comments.domain.model.CommentStatus
import com.mulkora.feature.comments.domain.repository.CommentsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Yorum repository gerçeklemesi.
 * CLAUDE.md §9: Yorumlar posts/{postId}/comments alt koleksiyonunda tutulur.
 * CLAUDE.md §26: Sayaç değişikliği ve yorum mutasyonları CF üzerinden yapılır.
 *
 * Okuma: Firestore gerçek zamanlı snapshot (callbackFlow)
 * Yazma: Cloud Functions (addComment, deleteComment, toggleCommentLike)
 */
@Singleton
class CommentsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) : CommentsRepository {

    companion object {
        private const val POSTS_COLLECTION    = "posts"
        private const val COMMENTS_COLLECTION = "comments"
    }

    // ─── Yorumları gerçek zamanlı dinle ──────────────────────────────────────

    override fun observeComments(postId: String): Flow<Result<List<Comment>>> = callbackFlow {
        trySend(Result.Loading)

        val currentUid = auth.currentUser?.uid

        val listener = firestore
            .collection(POSTS_COLLECTION)
            .document(postId)
            .collection(COMMENTS_COLLECTION)
            .whereEqualTo("parentId", null)          // Sadece üst düzey yorumlar
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToComment(doc.id, doc.data ?: return@mapNotNull null, currentUid)
                } ?: emptyList()
                trySend(Result.Success(comments))
            }

        awaitClose { listener.remove() }
    }

    // ─── Yorum ekleme (Cloud Function) ────────────────────────────────────────
    // CLAUDE.md §26: addComment CF içerik filtresinden geçirir

    override suspend fun addComment(
        postId: String,
        body: String,
        parentId: String?,
    ): Result<Unit> {
        if (auth.currentUser == null) {
            return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))
        }

        return try {
            val payload = hashMapOf<String, Any?>(
                "postId"   to postId,
                "body"     to body,
                "parentId" to parentId,
            )

            functions
                .getHttpsCallable("addComment")
                .call(payload)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Yorum silme (Cloud Function) ─────────────────────────────────────────
    // CLAUDE.md §26: deleteComment yalnızca sahibine/moderatöre izin verir

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        if (auth.currentUser == null) {
            return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))
        }

        return try {
            val payload = hashMapOf(
                "postId"    to postId,
                "commentId" to commentId,
            )

            functions
                .getHttpsCallable("deleteComment")
                .call(payload)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Beğeni toggle (Cloud Function) ───────────────────────────────────────
    // CLAUDE.md §26: likeCount sayacı istemciden değiştirilemez — CF üzerinden

    override suspend fun toggleCommentLike(postId: String, commentId: String): Result<Unit> {
        if (auth.currentUser == null) {
            return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))
        }

        return try {
            val payload = hashMapOf(
                "postId"    to postId,
                "commentId" to commentId,
            )

            functions
                .getHttpsCallable("toggleCommentLike")
                .call(payload)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ─── Yardımcı: Firestore dökümanı → Comment domain modeli ─────────────────

    private fun mapDocumentToComment(
        docId: String,
        data: Map<String, Any?>,
        currentUid: String?,
    ): Comment {
        val likedBy = (data["likedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val authorUid = data["authorUid"] as? String ?: ""

        return Comment(
            id                    = docId,
            postId                = data["postId"] as? String ?: "",
            parentId              = data["parentId"] as? String,
            authorUid             = authorUid,
            authorUsername        = data["authorUsername"] as? String ?: "",
            authorDisplayName     = data["authorDisplayName"] as? String ?: "",
            authorPhotoUrl        = data["authorPhotoUrl"] as? String,
            authorRole            = UserRole.fromValue(data["authorRole"] as? String ?: ""),
            content               = data["content"] as? String ?: "",
            likeCount             = (data["likeCount"] as? Long)?.toInt() ?: 0,
            replyCount            = (data["replyCount"] as? Long)?.toInt() ?: 0,
            isLikedByCurrentUser  = currentUid != null && currentUid in likedBy,
            isCurrentUserAuthor   = currentUid == authorUid,
            isHidden              = data["isHidden"] as? Boolean ?: false,
            status                = CommentStatus.fromValue(data["status"] as? String ?: "active"),
            createdAt             = data["createdAt"] as? com.google.firebase.Timestamp,
            updatedAt             = data["updatedAt"] as? com.google.firebase.Timestamp,
        )
    }
}