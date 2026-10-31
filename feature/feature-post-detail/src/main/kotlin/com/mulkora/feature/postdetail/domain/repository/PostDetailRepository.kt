package com.mulkora.feature.postdetail.domain.repository

import com.mulkora.core.common.result.Result
import com.mulkora.feature.postdetail.domain.model.PostDetail
import kotlinx.coroutines.flow.Flow

/**
 * Gönderi detayı modülü için repository arayüzü.
 *
 * CLAUDE.md §33: Compose ekranları doğrudan Firestore'a erişemez;
 * tüm okumalar bu repository üzerinden gerçekleştirilir.
 */
interface PostDetailRepository {

    /**
     * Belirtilen gönderinin detaylarını gerçek zamanlı olarak gözlemler.
     * Mevcut kullanıcının oy ve puan durumunu da içerir.
     */
    fun observePostDetail(postId: String): Flow<Result<PostDetail>>

    /**
     * Gönderiyi kaydedilen listesine ekler veya çıkarır.
     */
    suspend fun toggleSavePost(postId: String): Result<Unit>

    /**
     * Beğeni veya eksi oy tepkisi ekler/kaldırır.
     * vote: 1 = beğeni, -1 = eksi oy
     * CLAUDE.md §9: Tepki değişikliği Cloud Functions üzerinden atomik sayaç günceller.
     */
    suspend fun toggleVote(postId: String, vote: Int): Result<Unit>

    /**
     * Gönderiyi 1-5 yıldız arasında puanlar.
     * CLAUDE.md §9: Kullanıcı başına yalnızca bir puan verilebilir.
     */
    suspend fun ratePost(postId: String, rating: Int): Result<Unit>
}
