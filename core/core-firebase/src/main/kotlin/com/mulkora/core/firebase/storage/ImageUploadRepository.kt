package com.mulkora.core.firebase.storage

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.mulkora.core.common.result.Result
import com.mulkora.core.common.util.ExifStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage'a görsel yükleme işlemlerini yöneten repository.
 *
 * CLAUDE.md §15: Görsel Yükleme kuralları:
 * - MIME türü doğrulama
 * - Dosya boyutu sınırı (5 MB)
 * - Otomatik sıkıştırma
 * - EXIF konum verilerinin kaldırılması (ExifStripper)
 * - Güvenli dosya adı (UUID tabanlı)
 * - Yükleme başarısız olursa Storage'dan temizleme
 *
 * Storage yolları:
 * - Gönderi görselleri: posts/{uid}/{uuid}.jpg
 * - Profil fotoğrafları: profiles/{uid}/avatar.jpg
 */
@Singleton
class ImageUploadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) {

    companion object {
        /** Maksimum görsel boyutu: 5 MB */
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L

        /** İzin verilen MIME türleri */
        private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    /**
     * Gönderi görseli olarak Firebase Storage'a yükler.
     *
     * Akış:
     * 1. MIME türü doğrulama
     * 2. Boyut kontrolü
     * 3. EXIF konum verisi temizleme + sıkıştırma (ExifStripper)
     * 4. UUID tabanlı güvenli isimle Storage'a yükleme
     * 5. Download URL döndürme
     *
     * @param imageUri Galeri veya kameradan seçilen URI
     * @return Firebase Storage download URL veya hata
     */
    suspend fun uploadPostImage(imageUri: Uri): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok"))

        return withContext(Dispatchers.IO) {
            try {
                // 1. MIME türü kontrolü
                val mimeType = context.contentResolver.getType(imageUri)
                if (mimeType == null || mimeType !in ALLOWED_MIME_TYPES) {
                    return@withContext Result.Error(
                        IllegalArgumentException("Desteklenmeyen dosya türü: $mimeType. Yalnızca JPEG, PNG ve WebP desteklenir.")
                    )
                }

                // 2. Dosya boyutu kontrolü
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.Error(IllegalStateException("Görsel açılamadı"))
                val fileSize = inputStream.available().toLong()
                inputStream.close()

                if (fileSize > MAX_IMAGE_SIZE_BYTES) {
                    return@withContext Result.Error(
                        IllegalArgumentException("Görsel boyutu 5 MB'ı aşamaz")
                    )
                }

                // 3. EXIF temizleme + sıkıştırma
                val processedBytes = ExifStripper.stripAndCompress(context, imageUri)

                // 4. UUID tabanlı güvenli dosya adıyla Storage'a yükle
                val fileName = "${UUID.randomUUID()}.jpg"
                val storagePath = "posts/$uid/$fileName"
                val storageRef = storage.reference.child(storagePath)

                val uploadTask = storageRef
                    .putBytes(processedBytes)
                    .await()

                // 5. Download URL al
                val downloadUrl = storageRef.downloadUrl.await().toString()

                Result.Success(downloadUrl)
            } catch (e: Exception) {
                Result.Error(e, "Görsel yüklenemedi: ${e.message}")
            }
        }
    }

    /**
     * Profil fotoğrafını Firebase Storage'a yükler.
     *
     * Storage yolu: profiles/{uid}/avatar.jpg (sabit isim — her güncellemede üzerine yazar)
     *
     * @param imageUri Seçilen görsel URI
     * @return Firebase Storage download URL veya hata
     */
    suspend fun uploadProfilePhoto(imageUri: Uri): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok"))

        return withContext(Dispatchers.IO) {
            try {
                // MIME türü kontrolü
                val mimeType = context.contentResolver.getType(imageUri)
                if (mimeType == null || mimeType !in ALLOWED_MIME_TYPES) {
                    return@withContext Result.Error(
                        IllegalArgumentException("Desteklenmeyen dosya türü: $mimeType")
                    )
                }

                // EXIF temizleme + sıkıştırma
                val processedBytes = ExifStripper.stripAndCompress(context, imageUri)

                // Sabit ad: her güncellemede üzerine yazar
                val storageRef = storage.reference.child("profiles/$uid/avatar.jpg")

                storageRef.putBytes(processedBytes).await()

                val downloadUrl = storageRef.downloadUrl.await().toString()
                Result.Success(downloadUrl)
            } catch (e: Exception) {
                Result.Error(e, "Profil fotoğrafı yüklenemedi: ${e.message}")
            }
        }
    }

    /**
     * Storage'dan bir dosyayı siler.
     * Yükleme başarısız senaryolarında temizleme için kullanılır.
     *
     * @param downloadUrl Firebase Storage download URL
     */
    suspend fun deleteImage(downloadUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(downloadUrl)
            storageRef.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}