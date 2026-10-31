package com.mulkora.feature.reactions.domain.repository

import com.mulkora.feature.reactions.domain.model.PostReaction
import com.mulkora.feature.reactions.domain.model.ReactionType
import kotlinx.coroutines.flow.Flow

/**
 * Beğeni, eksi oy ve puanlama repository sözleşmesi.
 *
 * CLAUDE.md §10:
 * - Sayaçlar doğrudan istemci tarafından değiştirilemez. Cloud Functions kullanılır.
 * - Bir kullanıcı aynı gönderiye aynı anda hem beğeni hem eksi oy veremez.
 * - Her kullanıcı bir gönderiye yalnızca bir kez 1–5 yıldız arasında puan verebilir.
 */
interface ReactionsRepository {

    /**
     * Belirli bir gönderi için kullanıcının mevcut reaksiyonunu ve sayaçları gözlemler.
     * Firestore postReactions alt koleksiyonundan gerçek zamanlı dinler.
     */
    fun observePostReaction(postId: String): Flow<PostReaction>

    /**
     * Cloud Functions üzerinden beğeni veya eksi oy gönderir.
     * Mevcut reaksiyon aynıysa sıfırlanır (toggle davranışı).
     * Farklı tipte oy mevcutsa önce kaldırılır, sonra yeni tip eklenir.
     *
     * @param postId Reaksiyon verilen gönderi
     * @param type Gönderilecek tepki türü
     */
    suspend fun reactToPost(postId: String, type: ReactionType): Result<Unit>

    /**
     * Cloud Functions üzerinden puan gönderir.
     * Kullanıcı daha önce puan verdiyse günceller.
     *
     * @param postId Puanlanan gönderi
     * @param rating 1–5 arası puan
     */
    suspend fun ratePost(postId: String, rating: Int): Result<Unit>
}