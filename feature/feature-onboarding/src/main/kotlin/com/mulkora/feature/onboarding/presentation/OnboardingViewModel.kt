package com.mulkora.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.database.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding ViewModel — tamamlandı bayrağını DataStore'a yazar.
 *
 * CLAUDE.md §20: Onboarding durumu DataStore'da kalıcı olarak saklanır.
 * SplashViewModel onboarding tamamlanmadıysa bu ekranı, tamamlandıysa
 * Login'i göstermek için [UserPreferencesDataStore.onboardingCompletedFlow]'u okur.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : ViewModel() {

    /**
     * Kullanıcı onboarding'i tamamladı (son sayfa "Başla" ya da herhangi bir "Atla").
     * Bu yöntem çağrıldıktan sonra uygulama bir daha onboarding göstermez.
     */
    fun markOnboardingCompleted() {
        viewModelScope.launch {
            userPreferencesDataStore.setOnboardingCompleted(true)
        }
    }
}