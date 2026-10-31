package com.mulkora.feature.settings.di

import com.mulkora.feature.auth.domain.repository.AuthRepository
import com.mulkora.feature.billing.domain.repository.BillingRepository
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Settings feature DI modülü.
 *
 * CLAUDE.md §Mimari: Her feature kendi DI modülünü sağlar.
 * Settings feature'ı; AuthRepository, ProfileRepository ve BillingRepository'yi
 * diğer modüllerden consume eder — kendi binding'i yoktur.
 *
 * SettingsViewModel, ChangePasswordViewModel, DeleteAccountViewModel ve
 * DataDownloadViewModel bu modülün sağladığı bağımlılıkları @Inject ile alır.
 *
 * Kullanılan bağımlılıklar:
 *   - AuthRepository  → feature-auth modülünden (AuthModule)
 *   - ProfileRepository → feature-profile modülünden (ProfileModule)
 *   - BillingRepository → feature-billing modülünden (BillingModule)
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    // Bu modül yalnızca Hilt component scope'unu işaret eder.
    // AuthRepository, ProfileRepository ve BillingRepository bağlamaları
    // kendi modüllerinde (@Binds ile) zaten tanımlanmıştır.
    // SettingsViewModel constructor injection ile bu repository'leri alır.
}