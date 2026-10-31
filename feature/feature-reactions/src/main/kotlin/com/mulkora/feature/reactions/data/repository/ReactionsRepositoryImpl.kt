package com.mulkora.feature.reactions.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.feature.reactions.domain.model.PostReaction
import com.mulkora.feature.reactions.domain.model.ReactionType
import com.mulkora.feature.reactions.domain.repository.ReactionsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ReactionsRepository gerçek implementasyonu.
 *
 * CLAUDE.md §10: Sayaçlar doğrudan istemci tarafından değiştirilemez.
 * [reactToPost] ve [ratePost] Cloud Functions'a callable çağrı yapar.
 *
 * Firestore yapısı (Cloud Functions tanımıyla uyumlu):
 *   posts/{postId}                     → likeCount, dislikeCount, averageRating, ratingCount
 *   posts/{postId}/reactions/{uid}     → reactionType: "like" | "dislike"
 *   postRatings/{postId}_{uid}         → rating: 1–5  (flat belge, composite key)
 */
class ReactionsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) : ReactionsRepository {

    override fun observePostReaction(postId: String): Flow<PostReaction> = callbackFlow {
        val uid = auth.currentUser?.uid

        // Gönderi sayaçları (likeCount, dislikeCount, averageRating, ratingCount)
        val postRef = firestore.collection("posts").document(postId)

        // Kullanıcı reaksiyonu: posts/{postId}/reactions/{uid}
        val reactionRef = if (uid != null)
            firestore.collection("posts").document(postId)
                .collection("reactions").document(uid)
        else null

        // Kullanıcı puanı: postRatings/{postId}_{uid} (flat belge)
        val ratingRef = if (uid != null)
            firestore.collection("postRatings").document("${postId}_${uid}")
        else null

        var postData: Map<String, Any>? = null
        var reactionType: String? = null   // "like" | "dislike" | null
        var userRating: Int? = null

        fun emit() {
            val snap = postData ?: return
            trySend(
                PostReaction(
                    postId = postId,
                    userReaction = when (reactionType) {
                        "like" -> ReactionType.LIKE
                        "dislike" -> ReactionType.DISLIKE
                        else -> null
                    },
                    likeCount = (snap["likeCount"] as? Long)?.toInt() ?: 0,
                    dislikeCount = (snap["dislikeCount"] as? Long)?.toInt() ?: 0,
                    userRating = userRating,
                    averageRating = when (val v = snap["averageRating"]) {
                        is Double -> v.toFloat()
                        is Long -> v.toFloat()
                        else -> 0f
                    },
                    ratingCount = (snap["ratingCount"] as? Long)?.toInt() ?: 0,
                )
            )
        }

        val postListener = postRef.addSnapshotListener { snapshot, _ ->
            postData = snapshot?.data
            emit()
        }

        val reactionListener = reactionRef?.addSnapshotListener { snapshot, _ ->
            reactionType = snapshot?.getString("reactionType")
            emit()
        }

        val ratingListener = ratingRef?.addSnapshotListener { snapshot, _ ->
            userRating = (snapshot?.getLong("rating"))?.toInt()
            emit()
        }

        awaitClose {
            postListener.remove()
            reactionListener?.remove()
            ratingListener?.remove()
        }
    }

    /**
     * Cloud Functions `togglePostReaction` fonksiyonunu çağırır.
     *
     * Parametre uyumu:
     *   reactionType: "like" | "dislike"  (lowercase — Cloud Function beklentisi)
     */
    override suspend fun reactToPost(postId: String, type: ReactionType): Result<Unit> =
        runCatching {
            val data = mapOf(
                "postId" to postId,
                "reactionType" to type.name.lowercase(),  // "LIKE" → "like"
            )
            functions
                .getHttpsCallable("togglePostReaction")
                .call(data)
                .await()
            Unit
        }

    /**
     * Cloud Functions `ratePost` fonksiyonunu çağırır.
     *
     * @param rating 1–5 arası puan (Cloud Function server tarafında da doğrular)
     */
    override suspend fun ratePost(postId: String, rating: Int): Result<Unit> =
        runCatching {
            require(rating in 1..5) { "Puan 1–5 arasında olmalıdır." }
            val data = mapOf(
                "postId" to postId,
                "rating" to rating,
            )
            functions
                .getHttpsCallable("ratePost")
                .call(data)
                .await()
            Unit
        }
}