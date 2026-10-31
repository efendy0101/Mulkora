package com.mulkora.feature.billing.di

import com.mulkora.feature.billing.data.repository.BillingRepositoryImpl
import com.mulkora.feature.billing.domain.repository.BillingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Faturalandırma modülü DI bağlamaları.
 *
 * CLAUDE.md §18: BillingRepository Singleton olarak yaşam döngüsü yönetir
 * çünkü BillingClient bağlantısı uygulama boyunca tek olmak zorundadır.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingRepository(
        impl: BillingRepositoryImpl,
    ): BillingRepository
}