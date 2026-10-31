package com.mulkora.feature.ads

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Reklam modülü DI yapılandırması.
 *
 * CLAUDE.md §17: Reklam SDK'sı başlatma, FrequencyCapManager singleton olarak
 * tüm uygulamaya sağlanır. AdMob başlatma MulkoraApplication'da yapılır.
 */
@Module
@InstallIn(SingletonComponent::class)
object AdsModule {
    // FrequencyCapManager @Inject constructor ile @Singleton annotasyonuna sahip olduğundan
    // Hilt tarafından otomatik sağlanır; burada ekstra @Provides gerekmez.
    // AdMob Application ID google-services.json üzerinden okunur.
}