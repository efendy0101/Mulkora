package com.mulkora.feature.auth.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.mulkora.core.database.datastore.UserPreferencesDataStore
import com.mulkora.feature.auth.domain.repository.AuthRepository
import com.mulkora.feature.auth.domain.usecase.SyncFcmTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Splash ekranı ViewModel.
 * CLAUDE.md Bölüm 4: Kayıt ve Giriş Sistemi
 * Firebase Authentication oturumunu ve onboarding durumunu kontrol ederek
 * yönlendirme kararı verir.
 *
 * Navigasyon sırası:
 *   1. Onboarding henüz görülmediyse → Onboarding
 *   2. Oturum açılmışsa → Feed
 *   3. Oturum açılmamışsa → Login
 *
 * CLAUDE.md §16: Giriş yapılmış kullanıcı için FCM token Firestore'a senkronize edilir.
 * onNewToken yalnızca token yenilendiğinde tetiklenir; mevcut oturumlarda bu ViewModel
 * eksik token kaydını tamamlar.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncFcmTokenUseCase: SyncFcmTokenUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _navigationTarget = MutableStateFlow<SplashNavigationTarget?>(null)
    val navigationTarget: StateFlow<SplashNavigationTarget?> = _navigationTarget.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Onboarding daha önce tamamlanmadıysa önce onu göster
            val onboardingCompleted = userPreferencesDataStore.onboardingCompletedFlow.first()
            if (!onboardingCompleted) {
                _navigationTarget.value = SplashNavigationTarget.Onboarding
                return@launch
            }

            val currentUser = authRepository.currentUser.firstOrNull()
            if (currentUser != null) {
                // Kullanıcı oturum açmış — FCM token'ı arka planda senkronize et
                syncFcmTokenInBackground()
                _navigationTarget.value = SplashNavigationTarget.Feed
            } else {
                _navigationTarget.value = SplashNavigationTarget.Login
            }
        }
    }

    /**
     * Mevcut FCM token'ı alır ve WorkManager aracılığıyla Firestore'a yazar.
     * Hata durumu navigasyonu engellemez.
     */
    private fun syncFcmTokenInBackground() {
        viewModelScope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                syncFcmTokenUseCase(token)
            }
            // Hata sessizce görmezden gelinir; token bir sonraki onNewToken'da yeniden denenir
        }
    }
}

sealed interface SplashNavigationTarget {
    data object Feed : SplashNavigationTarget
    data object Login : SplashNavigationTarget
    data object Onboarding : SplashNavigationTarget
}