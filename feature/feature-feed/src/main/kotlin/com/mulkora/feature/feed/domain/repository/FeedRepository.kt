package com.mulkora.feature.feed.domain.repository

import androidx.paging.PagingData
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.feature.feed.domain.model.FeedItem
import com.mulkora.feature.feed.domain.model.Post
import com.mulkora.feature.feed.domain.model.ReactionType
import kotlinx.coroutines.flow.Flow

/**
 * Feed Repository interface.
 * CLAUDE.md Bölüm 7: Gönderi Oluşturma ve Keşfet
 */
interface FeedRepository {

    /**
     * Onaylı gönderileri sayfalı akış olarak döndürür.
     * Paging 3 ile Firestore pagination yapılır.
     * Reklam öğeleri burada eklenir (her N. sırada NativeAdItem).
     */
    fun getApprovedPostsPaged(
        category: PostCategory? = null,
        postType: PostType? = null,
        sortBy: FeedSortOption = FeedSortOption.NEWEST
    ): Flow<PagingData<FeedItem>>

    /**
     * Tek gönderiyi gerçek zamanlı olarak izler.
     */
    fun getPostById(postId: String): Flow<Result<Post?>>

    /**
     * Kullanıcının gönderilerini döndürür.
     */
    fun getPostsByUser(uid: String): Flow<Result<List<Post>>>

    /**
     * Gönderi oluşturur.
     * CLAUDE.md: Oluşturma sonrası durum "draft" olarak ayarlanır.
     * submitPost Cloud Function'ı "pending" durumuna geçirir.
     */
    suspend fun createPost(
        title: String,
        content: String,
        postType: PostType,
        category: PostCategory,
        tags: List<String>,
        imageUri: String? = null
    ): Result<String> // postId döndürür

    /**
     * Gönderiyi günceller.
     * CLAUDE.md: Yalnızca draft veya edit_requested durumunda güncellenebilir.
     */
    suspend fun updatePost(
        postId: String,
        title: String,
        content: String,
        postType: PostType,
        category: PostCategory,
        tags: List<String>,
        imageUri: String? = null
    ): Result<Unit>

    /**
     * Taslağı incelemeye gönderir (draft → pending).
     * submitPost Cloud Function çağrılır.
     * CLAUDE.md §8: Tüm gönderiler moderasyon kuyruğuna alınır.
     */
    suspend fun submitPost(postId: String): Result<Unit>

    /**
     * Gönderiyi siler.
     * CLAUDE.md §26: Silme yalnızca admin/CF üzerinden yapılabilir; istemci sadece kendi taslağını silebilir.
     */
    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * Gönderi tepkisi ekler/kaldırır.
     * CLAUDE.md: Sayaç değişikliği Cloud Functions üzerinden yapılır.
     */
    suspend fun toggleReaction(postId: String, reactionType: ReactionType): Result<Unit>

    /**
     * Gönderiyi puanlar (1-5 yıldız).
     */
    suspend fun ratePost(postId: String, rating: Int): Result<Unit>

    /**
     * Gönderiyi kaydeder / kayıttan kaldırır.
     */
    suspend fun toggleSavePost(postId: String): Result<Unit>

    /**
     * Kaydedilen gönderileri döndürür.
     */
    fun getSavedPosts(): Flow<Result<List<Post>>>
}

/**
 * Feed sıralama seçenekleri.
 * CLAUDE.md §6: En yeni, en çok beğenilen, en çok yorumlanan, en yüksek puanlı.
 */
enum class FeedSortOption(val label: String) {
    NEWEST("En Yeni"),
    POPULAR("En Çok Beğenilen"),
    MOST_COMMENTED("En Çok Yorumlanan"),
    TOP_RATED("En Yüksek Puanlı")
}
