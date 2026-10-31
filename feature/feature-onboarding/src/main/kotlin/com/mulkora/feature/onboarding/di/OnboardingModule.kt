package com.mulkora.feature.onboarding.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Onboarding feature DI modülü.
 *
 * CLAUDE.md §Mimari: Her feature kendi DI modülünü sağlar.
 *
 * OnboardingViewModel, yalnızca UserPreferencesDataStore'a ihtiyaç duyar.
 * Bu bağımlılık core-database modülündeki DatabaseModule tarafından @Singleton
 * olarak sağlanır; burada tekrar tanımlanmasına gerek yoktur.
 *
 * Gelecekte onboarding veri kaynağı (remote config, Firestore vb.) eklenirse
 * @Binds / @Provides buraya eklenir.
 */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule