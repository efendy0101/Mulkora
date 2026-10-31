package com.mulkora.feature.auth.domain.repository

import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Auth Repository interface.
 * CLAUDE.md Bölüm 2: Kimlik Doğrulama
 *
 * Clean Architecture kuralı: Domain katmanı implementation bilmez.
 * Implementasyon data katmanında (AuthRepositoryImpl) yapılır.
 */
interface AuthRepository {

    /**
     * Mevcut oturum açmış kullanıcıyı döndürür.
     * Oturum değişikliklerini gerçek zamanlı izler.
     */
    val currentUser: Flow<AuthUser?>

    /**
     * Kullanıcı oturum açmış mı?
     */
    val isUserLoggedIn: Boolean

    /**
     * E-posta ve şifre ile giriş yapar.
     * CLAUDE.md: E-posta doğrulama zorunludur.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>

    /**
     * Google ile giriş yapar.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /**
     * E-posta ve şifre ile kayıt olur.
     * CLAUDE.md: Kayıt sonrası e-posta doğrulama gönderilir.
     */
    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser>

    /**
     * Doğrulama e-postası gönderir.
     */
    suspend fun sendEmailVerification(): Result<Unit>

    /**
     * Şifre sıfırlama e-postası gönderir.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Şifreyi değiştirir (mevcut şifre doğrulandıktan sonra).
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /**
     * Oturumu kapatır.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Hesabı siler.
     * CLAUDE.md: Hesap silme işlemi Cloud Functions üzerinden yapılır.
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * E-postanın doğrulanıp doğrulanmadığını kontrol eder.
     * Firebase token yenile ve kontrol et.
     */
    suspend fun reloadUser(): Result<AuthUser>

    /**
     * Kullanıcı adının müsait olup olmadığını kontrol eder.
     * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden güvenli biçimde kontrol edilmelidir.
     * Firestore /usernames/{username} belgesine bakar.
     */
    suspend fun isUsernameAvailable(username: String): Boolean

    /**
     * Kullanıcı adını günceller.
     * Firestore /usernames/{username} ve /users/{uid}.username alanlarını atomik olarak yazar.
     * @return İşlem başarılı ise true, hata varsa false.
     */
    suspend fun updateUsername(username: String): Boolean
}
