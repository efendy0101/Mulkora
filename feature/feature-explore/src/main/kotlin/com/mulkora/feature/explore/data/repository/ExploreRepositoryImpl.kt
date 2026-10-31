package com.mulkora.feature.explore.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostStatus
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.core.firebase.util.FirebaseCollections
import com.mulkora.feature.explore.domain.model.ExplorePost
import com.mulkora.feature.explore.domain.repository.CategorySection
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * ExploreRepository Firestore implementasyonu.
 *
 * CLAUDE.md §7: Yalnızca status=APPROVED gönderiler Keşfet listelerinde gösterilir.
 * Güvenlik kuralları ile desteklenir (firestore.rules).
 */
class ExploreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ExploreRepository {

    override fun observeFeaturedCategories(): Flow<Result<List<CategorySection>>> = flow {
        emit(Result.Loading)
        // Şimdilik sadece kategorileri listele, ileride her kategoriden post çekilebilir
        val sections = PostCategory.entries.map { category ->
            CategorySection(category, emptyList())
        }
        emit(Result.Success(sections))
    }

    override fun observePopularCities(): Flow<Result<List<String>>> = flow {
        emit(Result.Loading)
        // Örnek popüler şehirler
        val cities = listOf("İstanbul", "Ankara", "İzmir", "Bursa", "Antalya")
        emit(Result.Success(cities))
    }

    override fun getPostsByCategory(category: PostCategory): Flow<Result<List<ExplorePost>>> =
        callbackFlow {
            trySend(Result.Loading)
            val listener = firestore
                .collection(FirebaseCollections.POSTS)
                .whereEqualTo("category", category.value)
                .whereEqualTo("status", PostStatus.APPROVED.value)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }
                    val posts = snapshot?.documents?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        ExplorePost(
                            id = doc.id,
                            title = data["title"] as? String ?: return@mapNotNull null,
                            category = PostCategory.fromValue(data["category"] as? String ?: ""),
                            type = PostType.fromValue(data["type"] as? String ?: ""),
                            city = data["city"] as? String,
                            authorDisplayName = if (data["isAnonymous"] as? Boolean == true)
                                "Anonim"
                            else
                                data["authorDisplayName"] as? String ?: "Bilinmeyen",
                            authorPhotoUrl = data["authorPhotoUrl"] as? String,
                            likeCount = (data["likeCount"] as? Long)?.toInt() ?: 0,
                            commentCount = (data["commentCount"] as? Long)?.toInt() ?: 0,
                            averageRating = (data["averageRating"] as? Double) ?: 0.0,
                            thumbnailUrl = data["thumbnailUrl"] as? String,
                            createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                        )
                    } ?: emptyList()
                    trySend(Result.Success(posts))
                }
            awaitClose { listener.remove() }
        }

    override fun getPostsByCity(city: String): Flow<Result<List<ExplorePost>>> = callbackFlow {
        trySend(Result.Loading)
        val listener = firestore
            .collection(FirebaseCollections.POSTS)
            .whereEqualTo("city", city.lowercase())
            .whereEqualTo("status", PostStatus.APPROVED.value)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ExplorePost(
                        id = doc.id,
                        title = data["title"] as? String ?: return@mapNotNull null,
                        category = PostCategory.fromValue(data["category"] as? String ?: ""),
                        type = PostType.fromValue(data["type"] as? String ?: ""),
                        city = data["city"] as? String,
                        authorDisplayName = if (data["isAnonymous"] as? Boolean == true)
                            "Anonim"
                        else
                            data["authorDisplayName"] as? String ?: "Bilinmeyen",
                        authorPhotoUrl = data["authorPhotoUrl"] as? String,
                        likeCount = (data["likeCount"] as? Long)?.toInt() ?: 0,
                        commentCount = (data["commentCount"] as? Long)?.toInt() ?: 0,
                        averageRating = (data["averageRating"] as? Double) ?: 0.0,
                        thumbnailUrl = data["thumbnailUrl"] as? String,
                        createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                    )
                } ?: emptyList()
                trySend(Result.Success(posts))
            }
        awaitClose { listener.remove() }
    }
}
