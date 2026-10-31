package com.mulkora.feature.explore.domain.repository

import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.result.Result
import com.mulkora.feature.explore.domain.model.ExplorePost
import kotlinx.coroutines.flow.Flow

/**
 * Keşfet ekranı kategori bölümü.
 */
data class CategorySection(
    val category: PostCategory,
    val posts: List<ExplorePost>
)

/**
 * Keşfet modülü repository arayüzü.
 *
 * CLAUDE.md §33: Compose ekranları doğrudan Firestore'a erişemez.
 */
interface ExploreRepository {

    /**
     * Öne çıkan kategorileri ve her kategoriden birkaç gönderiyi getirir.
     */
    fun observeFeaturedCategories(): Flow<Result<List<CategorySection>>>

    /**
     * Popüler şehirleri getirir.
     */
    fun observePopularCities(): Flow<Result<List<String>>>

    /**
     * Belirtilen kategoriye ait onaylı gönderileri getirir.
     * CLAUDE.md §7: Yalnızca status=APPROVED gönderiler listelenir.
     */
    fun getPostsByCategory(category: PostCategory): Flow<Result<List<ExplorePost>>>

    /**
     * Belirtilen şehre ait onaylı gönderileri getirir.
     */
    fun getPostsByCity(city: String): Flow<Result<List<ExplorePost>>>
}