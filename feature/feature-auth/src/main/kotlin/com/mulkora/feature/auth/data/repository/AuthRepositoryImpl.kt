package com.mulkora.feature.auth.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.model.AuthUser
import com.mulkora.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override val isUserLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.Error(Exception("Kullanıcı bulunamadı"))
            Result.Success(user.toAuthUser())
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(e, "E-posta veya şifre hatalı")
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(e, "Bu e-posta ile kayıtlı kullanıcı bulunamadı")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.Error(Exception("Google girişi başarısız"))
            Result.Success(user.toAuthUser())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.Error(Exception("Kayıt oluşturulamadı"))
            // E-posta doğrulama gönder
            user.sendEmailVerification().await()
            Result.Success(user.toAuthUser())
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.Error(e, "Şifre çok zayıf. En az 6 karakter kullanın")
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.Error(e, "Bu e-posta ile zaten kayıtlı bir hesap var")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(e, "Geçersiz e-posta adresi")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.Error(Exception("Oturum açık değil"))
            user.sendEmailVerification().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(e, "Bu e-posta ile kayıtlı kullanıcı bulunamadı")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.Error(Exception("Oturum açık değil"))
            val email = user.email
                ?: return Result.Error(Exception("E-posta bulunamadı"))

            // Re-authenticate (yeniden kimlik doğrulama — şifre değişikliği için zorunlu)
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(e, "Mevcut şifre hatalı")
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.Error(e, "Yeni şifre çok zayıf")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            // Hesap silme Cloud Functions deleteAccount üzerinden yapılır
            // Bu metod sadece Firebase Auth'tan siler; Firestore verileri CF tarafından temizlenir
            firebaseAuth.currentUser?.delete()?.await()
                ?: return Result.Error(Exception("Oturum açık değil"))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun reloadUser(): Result<AuthUser> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.Error(Exception("Oturum açık değil"))
            user.reload().await()
            Result.Success(user.toAuthUser())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Kullanıcı adı müsait mi?
     * checkUsername Cloud Function ile backend doğrulaması yapılır.
     * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden güvenli biçimde kontrol edilmeli.
     */
    override suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val result = functions
                .getHttpsCallable("checkUsername")
                .call(mapOf("username" to username))
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any> ?: return false
            data["available"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kullanıcı adını Cloud Function üzerinden atomik olarak rezerve eder.
     * reserveUsername CF: transaction ile race-condition güvenli, /usernames yazan yalnızca CF.
     *
     * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden güvenli biçimde kontrol edilmeli.
     * CLAUDE.md §26: /usernames koleksiyonu istemci tarafından yazılamaz.
     * Firestore Rules: /usernames → allow write: if false
     */
    override suspend fun updateUsername(username: String): Boolean {
        return try {
            val result = functions
                .getHttpsCallable("reserveUsername")
                .call(mapOf("username" to username))
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any> ?: return false
            data["success"] as? Boolean ?: false
        } catch (e: FirebaseFunctionsException) {
            // already-exists → kullanıcı adı alınmış
            false
        } catch (e: Exception) {
            false
        }
    }
}

// Extension: FirebaseUser → AuthUser
private fun com.google.firebase.auth.FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
    isEmailVerified = isEmailVerified,
    isAnonymous = isAnonymous
)