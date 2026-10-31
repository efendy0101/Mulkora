package com.mulkora.feature.profile.domain.repository

import android.net.Uri
import com.mulkora.core.common.result.Result
import com.mulkora.feature.profile.domain.model.BlockedUser
import com.mulkora.feature.profile.domain.model.UpdateProfileRequest
import com.mulkora.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Profil Repository interface.
 * CLAUDE.md Bölüm 3: Kullanıcı Profili ve Ayarlar
 */
interface ProfileRepository {

    /**
     * Mevcut oturum açmış kullanıcının profilini gerçek zamanlı izler.
     */
    val currentUserProfile: Flow<Result<UserProfile?>>

    /**
     * Herhangi bir kullanıcının profilini getirir.
     */
    fun getUserProfile(uid: String): Flow<Result<UserProfile?>>

    /**
     * Kullanıcı adına göre profil getirir.
     */
    suspend fun getUserByUsername(username: String): Result<UserProfile?>

    /**
     * Profili günceller.
     * CLAUDE.md: Rol, satın alma durumu ve sayaçlar değiştirilemez.
     */
    suspend fun updateProfile(request: UpdateProfileRequest): Result<Unit>

    /**
     * Profil fotoğrafını günceller (ham URI ile).
     * Storage'a yükler, URL'i Firestore'a kaydeder.
     *
     * ÖNEMLİ: Çağırmadan önce ExifStripper.stripAndCompress() kullanınız.
     * Tercih edilen yöntem updateProfilePhotoBytes() — EXIF temizleme garantisi verir.
     */
    suspend fun updateProfilePhoto(imageUri: Uri): Result<String>

    /**
     * Profil fotoğrafını günceller (EXIF temizlenmiş bayt dizisi ile).
     *
     * CLAUDE.md §16 güvenlik gereği: Bu metot, ExifStripper tarafından
     * GPS ve kişisel metadata temizlenmiş ByteArray alır.
     *
     * @param imageBytes ExifStripper.stripAndCompress() çıktısı
     * @return Yeni Firebase Storage download URL'si
     */
    suspend fun updateProfilePhotoBytes(imageBytes: ByteArray): Result<String>

    /**
     * Profil fotoğrafını siler.
     */
    suspend fun deleteProfilePhoto(): Result<Unit>

    /**
     * Kullanıcı adı müsait mi?
     */
    suspend fun checkUsernameAvailability(username: String): Result<Boolean>

    /**
     * Kullanıcıyı engeller.
     * CLAUDE.md: blockUser Cloud Function üzerinden çalışır.
     */
    suspend fun blockUser(targetUid: String): Result<Unit>

    /**
     * Kullanıcı engelini kaldırır.
     */
    suspend fun unblockUser(targetUid: String): Result<Unit>

    /**
     * Engellenen kullanıcıları listeler.
     */
    suspend fun getBlockedUsers(): Result<List<BlockedUser>>

    /**
     * İçerik şikayeti bildirir.
     */
    suspend fun reportUser(
        targetUid: String,
        reason: String,
        details: String? = null
    ): Result<Unit>

    /**
     * CLAUDE.md §27: Kullanıcı kendi verilerini indirmek için talepte bulunur.
     * Cloud Function `requestDataExport` güvenli biçimde tetiklenir.
     * Dışa aktarma tamamlandığında kullanıcıya bildirim gönderilir.
     */
    suspend fun requestDataExport(): Result<Unit>
}
