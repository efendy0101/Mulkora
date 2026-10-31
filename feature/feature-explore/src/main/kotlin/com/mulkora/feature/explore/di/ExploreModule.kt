package com.mulkora.feature.explore.di

import com.mulkora.feature.explore.data.repository.ExploreRepositoryImpl
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Keşfet modülü Hilt bağımlılık modülü.
 *
 * CLAUDE.md §33: Repository arayüzü implementasyona bind edilir.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExploreModule {

    @Binds
    @Singleton
    abstract fun bindExploreRepository(
        impl: ExploreRepositoryImpl,
    ): ExploreRepository
}