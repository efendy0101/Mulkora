package com.mulkora.feature.feed.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mulkora.feature.feed.data.dto.PostDto
import com.mulkora.feature.feed.domain.model.Post
import com.mulkora.feature.feed.domain.repository.FeedSortOption
import kotlinx.coroutines.tasks.await

/**
 * Paging 3 + Firestore cursor tabanlı sayfalandırma kaynağı.
 * docs/13-firestore-indexes.md: Feed sorgusuna karşılık gelen bileşik indeksler kullanılır.
 *
 * Cursor: Son belgenin snapshot'ı ile sonraki sayfa sorgulanır.
 */
class FeedPagingSource(
    private val firestore: FirebaseFirestore,
    private val sortOption: FeedSortOption,
    private val categoryFilter: String?,
    private val typeFilter: String?,
) : PagingSource<DocumentSnapshot, Post>() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val POSTS_COLLECTION = "posts"
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Post>): DocumentSnapshot? = null

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Post> {
        return try {
            var query: Query = firestore
                .collection(POSTS_COLLECTION)
                .whereEqualTo("status", "approved")

            // Kategori filtresi
            if (!categoryFilter.isNullOrEmpty()) {
                query = query.whereEqualTo("category", categoryFilter)
            }

            // Tür filtresi
            if (!typeFilter.isNullOrEmpty()) {
                query = query.whereEqualTo("postType", typeFilter)
            }

            // Sıralama
            // docs/13-firestore-indexes.md: Her case için uygun bileşik index gereklidir.
            query = when (sortOption) {
                FeedSortOption.NEWEST -> query.orderBy("createdAt", Query.Direction.DESCENDING)
                FeedSortOption.POPULAR -> query.orderBy("likeCount", Query.Direction.DESCENDING)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                FeedSortOption.MOST_COMMENTED -> query.orderBy("commentCount", Query.Direction.DESCENDING)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                FeedSortOption.TOP_RATED -> query.orderBy("averageRating", Query.Direction.DESCENDING)
                    .orderBy("ratingCount", Query.Direction.DESCENDING)
            }

            // Cursor tabanlı sayfalandırma
            val cursor = params.key
            if (cursor != null) {
                query = query.startAfter(cursor)
            }

            query = query.limit(PAGE_SIZE.toLong())

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PostDto::class.java)?.toPost()
            }

            val nextKey = if (snapshot.documents.size < PAGE_SIZE) {
                null
            } else {
                snapshot.documents.lastOrNull()
            }

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}