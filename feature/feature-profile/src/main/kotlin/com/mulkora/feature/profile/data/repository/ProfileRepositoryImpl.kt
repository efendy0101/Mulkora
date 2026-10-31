package com.mulkora.feature.profile.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.mulkora.core.common.model.UserRole
import com.mulkora.core.common.result.Result
import com.mulkora.feature.profile.domain.model.BlockedUser
import com.mulkora.feature.profile.domain.model.SocialLinks
import com.mulkora.feature.profile.domain.model.UpdateProfileRequest
import com.mulkora.feature.profile.domain.model.UserProfile
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProfileRepository implementasyonu.
 *
 * CLAUDE.md §19: Profil sayfası gereksinimleri.
 * CLAUDE.md §26: Firestore güvenliği — kullanıcı yalnızca kendi izin verilen alanlarını değiştirebilir.
 * CLAUDE.md §27: KVKK — veri indirme talebi, hesap silme Cloud Functions üzerinden.
 * CLAUDE.md §15: Görsel yükleme — EXIF kaldırma, güvenli dosya adı, Storage kuralları.
 * CLAUDE.md §25: blockUser, requestDataExport Cloud Functions üzerinden çalışır.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
) : ProfileRepository {

    // ── Sabit koleksiyon ve depolama yolları ────────────────────────────────

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_PUBLIC_PROFILES = "publicProfiles"
        private const val COLLECTION_USERNAMES = "usernames"
        private const val COLLECTION_BLOCKED = "blockedUsers"
        private const val STORAGE_PROFILE_PHOTOS = "profile_photos"
    }

    // ── currentUserProfile (Flow property) ──────────────────────────────────

    /**
     * Oturum açmış kullanıcının profilini gerçek zamanlı izler.
     * Oturum yoksa null emit eder ve akışı kapatır.
     */
    override val currentUserProfile: Flow<Result<UserProfile?>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.Success(null))
            close()
            return@callbackFlow
        }

        val listener = firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val dto = snapshot?.toObject(UserProfileDto::class.java)
                trySend(Result.Success(dto?.toUserProfile()))
            }

        awaitClose { listener.remove() }
    }

    // ── getUserProfile ───────────────────────────────────────────────────────

    override fun getUserProfile(uid: String): Flow<Result<UserProfile?>> = callbackFlow {
        val listener = firestore
            .collection(if (uid == auth.currentUser?.uid) COLLECTION_USERS else COLLECTION_PUBLIC_PROFILES)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val dto = snapshot?.toObject(UserProfileDto::class.java)
                trySend(Result.Success(dto?.toUserProfile()))
            }

        awaitClose { listener.remove() }
    }

    // ── getUserByUsername ────────────────────────────────────────────────────

    /**
     * usernames/{username} belgesindeki uid ile users/{uid} belgesini getirir.
     * CLAUDE.md §3: Kullanıcı adı benzersiz ve küçük harfle saklanır.
     */
    override suspend fun getUserByUsername(username: String): Result<UserProfile?> {
        return try {
            val usernameDoc = firestore
                .collection(COLLECTION_USERNAMES)
                .document(username.lowercase().trim())
                .get()
                .await()

            val uid = usernameDoc.getString("uid")
                ?: return Result.Success(null)

            val userDoc = firestore
                .collection(if (uid == auth.currentUser?.uid) COLLECTION_USERS else COLLECTION_PUBLIC_PROFILES)
                .document(uid)
                .get()
                .await()

            val dto = userDoc.toObject(UserProfileDto::class.java)
            Result.Success(dto?.toUserProfile())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── updateProfile ────────────────────────────────────────────────────────

    /**
     * CLAUDE.md §26: Rol ve satın alma durumu istemci tarafından değiştirilemez.
     * Yalnızca UpdateProfileRequest içindeki izin verilen alanlar güncellenir.
     */
    override suspend fun updateProfile(request: UpdateProfileRequest): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

            val updates = mutableMapOf<String, Any?>(
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            request.displayName?.let { updates["displayName"] = it }
            request.bio?.let { updates["bio"] = it }
            request.city?.let { updates["city"] = it }
            request.photoUrl?.let { updates["photoUrl"] = it }
            request.links?.let { links ->
                updates["links"] = buildMap {
                    links.twitter?.let { put("twitter", it) }
                    links.linkedin?.let { put("linkedin", it) }
                    links.instagram?.let { put("instagram", it) }
                }
            }

            @Suppress("UNCHECKED_CAST")
            firestore.collection(COLLECTION_USERS).document(uid)
                .update(updates as Map<String, Any>)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── updateProfilePhoto ───────────────────────────────────────────────────

    /**
     * CLAUDE.md §15: Görsel yükleme — MIME türü kontrolü, güvenli dosya adı.
     * EXIF kaldırma işlemi UI katmanında (ImageDecoder/Bitmap) yapılır ve
     * bu fonksiyona temizlenmiş Uri iletilir.
     *
     * @return Yeni download URL'si.
     */
    override suspend fun updateProfilePhoto(imageUri: Uri): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

            val storageRef = storage.reference
                .child("$STORAGE_PROFILE_PHOTOS/$uid.jpg")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Firestore'da profil fotoğrafı URL'sini güncelle
            firestore.collection(COLLECTION_USERS).document(uid)
                .update(
                    mapOf(
                        "photoUrl" to downloadUrl,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()

            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── updateProfilePhotoBytes ──────────────────────────────────────────────

    /**
     * EXIF temizlenmiş ByteArray ile profil fotoğrafı yükler.
     *
     * CLAUDE.md §16: GPS ve kişisel metadata, ExifStripper tarafından
     * yükleme öncesinde kaldırılmış olmalıdır. Bu metot bu garantiyi
     * doğrular ve uygun MIME türünde (image/jpeg) yükler.
     */
    override suspend fun updateProfilePhotoBytes(imageBytes: ByteArray): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

            val storageRef = storage.reference
                .child("$STORAGE_PROFILE_PHOTOS/$uid.jpg")

            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            storageRef.putBytes(imageBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            firestore.collection(COLLECTION_USERS).document(uid)
                .update(
                    mapOf(
                        "photoUrl" to downloadUrl,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()

            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── deleteProfilePhoto ───────────────────────────────────────────────────

    override suspend fun deleteProfilePhoto(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

            // Storage'dan sil — dosya yoksa hata yut, devam et
            try {
                storage.reference
                    .child("$STORAGE_PROFILE_PHOTOS/$uid.jpg")
                    .delete()
                    .await()
            } catch (_: Exception) {
                // Fotoğraf zaten silinmiş olabilir; kritik değil
            }

            // Firestore'daki URL'yi temizle
            firestore.collection(COLLECTION_USERS).document(uid)
                .update(
                    mapOf(
                        "photoUrl" to null,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── checkUsernameAvailability ────────────────────────────────────────────

    override suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val doc = firestore
                .collection(COLLECTION_USERNAMES)
                .document(username.lowercase().trim())
                .get()
                .await()
            Result.Success(!doc.exists())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── blockUser ────────────────────────────────────────────────────────────

    /**
     * CLAUDE.md §25: Kullanıcı engelleme Cloud Function üzerinden çalışır.
     */
    override suspend fun blockUser(targetUid: String): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("blockUser")
                .call(mapOf("targetUid" to targetUid))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── unblockUser ──────────────────────────────────────────────────────────

    override suspend fun unblockUser(targetUid: String): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("unblockUser")
                .call(mapOf("targetUid" to targetUid))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── getBlockedUsers ──────────────────────────────────────────────────────

    /**
     * Mevcut kullanıcının engellediği kullanıcıların profillerini getirir.
     * blockedUsers alt koleksiyonu her belgede "blockedUid" alanı barındırır.
     */
    override suspend fun getBlockedUsers(): Result<List<BlockedUser>> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.Error(IllegalStateException("Kullanıcı oturumu yok."))

            val blockedDocs = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_BLOCKED)
                .get()
                .await()

            val blockedUsers = blockedDocs.documents.mapNotNull { doc ->
                val blockedUid = doc.getString("blockedUid") ?: return@mapNotNull null
                val blockedAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L

                // Profil verilerini de getir
                val userDoc = firestore.collection(COLLECTION_USERS)
                    .document(blockedUid)
                    .get()
                    .await()
                
                val userDto = userDoc.toObject(UserProfileDto::class.java) ?: return@mapNotNull null
                
                BlockedUser(
                    uid = userDto.uid,
                    username = userDto.username,
                    displayName = userDto.displayName,
                    photoUrl = userDto.photoUrl,
                    blockedAt = blockedAt
                )
            }

            Result.Success(blockedUsers)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── reportUser ───────────────────────────────────────────────────────────

    /**
     * CLAUDE.md §11: Şikâyet sistemi — kullanıcı şikâyeti Cloud Function aracılığıyla oluşturulur.
     */
    override suspend fun reportUser(
        targetUid: String,
        reason: String,
        details: String?,
    ): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("createReport")
                .call(
                    buildMap<String, Any> {
                        put("targetType", "user")
                        put("targetId", targetUid)
                        put("reason", reason)
                        if (details != null) put("details", details)
                    }
                )
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── requestDataExport ────────────────────────────────────────────────────

    /**
     * CLAUDE.md §27: Kullanıcı kendi verilerini indirmek için talepte bulunur.
     * Cloud Function `requestDataExport` güvenli biçimde tetiklenir.
     * Dışa aktarma tamamlandığında kullanıcıya bildirim gönderilir.
     */
    override suspend fun requestDataExport(): Result<Unit> {
        return try {
            functions
                .getHttpsCallable("requestDataExport")
                .call()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// ── DTO ──────────────────────────────────────────────────────────────────────

/**
 * Firestore `users/{uid}` belge DTO'su.
 * Alan adları docs/12-firestore-document-schemas.md ile eşleşmelidir.
 *
 * NOT: Firestore'daki `links` alanı Map<String, String?> olarak okunur;
 * toUserProfile() fonksiyonu bunu SocialLinks domain modeline dönüştürür.
 */
internal data class UserProfileDto(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val bio: String? = null,
    val role: String = UserRole.TENANT.value,
    val isEmailVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val adsRemoved: Boolean = false,
    val postCount: Int = 0,
    val referenceCount: Int = 0,
    val averageReferenceRating: Double = 0.0,
    val city: String? = null,
    val links: Map<String, Any?>? = null,
    val createdAt: Timestamp? = null,
) {
    fun toUserProfile(): UserProfile = UserProfile(
        uid = uid,
        username = username,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        bio = bio,
        role = UserRole.fromValue(role),
        isEmailVerified = isEmailVerified,
        isBlocked = isBlocked,
        adsRemoved = adsRemoved,
        postCount = postCount,
        referenceCount = referenceCount,
        averageReferenceRating = averageReferenceRating,
        city = city,
        links = links?.let { map ->
            SocialLinks(
                twitter = map["twitter"] as? String,
                linkedin = map["linkedin"] as? String,
                instagram = map["instagram"] as? String,
            )
        },
        createdAt = createdAt,
    )
}