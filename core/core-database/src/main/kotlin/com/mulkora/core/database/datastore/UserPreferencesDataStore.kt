package com.mulkora.core.database.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulama genelinde kullanıcı tercihlerini DataStore üzerinden yönetir.
 *
 * CLAUDE.md §18: Reklam kaldırma satın alma durumu hem Firestore'da (doğrulama)
 * hem de yerel DataStore'da (hız için önbellek) tutulur.
 * CLAUDE.md §20: Tema ve onboarding gibi kullanıcı tercihleri burada saklanır.
 *
 * Veriler şifrelenmez; hassas veriler (satın alma tokenı) burada tutulmaz,
 * yalnızca erişim bayrağı saklanır.
 */
private val Context.userPreferencesDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "mulkora_user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val dataStore = context.userPreferencesDataStore

    companion object {
        // Satın alma — CLAUDE.md §18
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")

        // Onboarding görüldü mü
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Tema tercihi: "light" | "dark" | "system"
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")

        // Giriş yapan kullanıcının UID'si (Splash optimizasyonu için)
        val LAST_USER_UID = stringPreferencesKey("last_user_uid")
    }

    // ============================================================
    // Reklam kaldırma satın alması
    // ============================================================

    /**
     * Kullanıcı reklamları kaldırdı mı? (yerel önbellek)
     *
     * Gerçek kaynak Firestore'daki purchases/{uid} belgesidir;
     * bu değer yalnızca hız amaçlı önbellektir.
     */
    val adsRemovedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ADS_REMOVED] ?: false
    }

    /**
     * Satın alma doğrulandıktan sonra yerel bayrağı günceller.
     * Yalnızca Cloud Function doğrulaması başarılı olduğunda çağrılır.
     */
    suspend fun setAdsRemoved(removed: Boolean) {
        dataStore.edit { prefs ->
            prefs[ADS_REMOVED] = removed
        }
    }

    // ============================================================
    // Onboarding
    // ============================================================

    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    // ============================================================
    // Tema tercihi
    // ============================================================

    val themePreferenceFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_PREFERENCE] ?: "system"
    }

    suspend fun setThemePreference(theme: String) {
        dataStore.edit { prefs ->
            prefs[THEME_PREFERENCE] = theme
        }
    }

    // ============================================================
    // Son giriş UID
    // ============================================================

    val lastUserUidFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[LAST_USER_UID]
    }

    suspend fun setLastUserUid(uid: String?) {
        dataStore.edit { prefs ->
            if (uid != null) {
                prefs[LAST_USER_UID] = uid
            } else {
                prefs.remove(LAST_USER_UID)
            }
        }
    }

    // ============================================================
    // Tüm veriyi temizle (hesap silme)
    // CLAUDE.md §27: Kullanıcı verisi silinme akışı
    // ============================================================

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}