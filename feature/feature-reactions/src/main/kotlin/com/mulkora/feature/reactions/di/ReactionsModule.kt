package com.mulkora.feature.reactions.di

import com.mulkora.feature.reactions.data.repository.ReactionsRepositoryImpl
import com.mulkora.feature.reactions.domain.repository.ReactionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI modülü — Reactions feature bağımlılık bağlamaları.
 *
 * CLAUDE.md §23: Her feature modülü kendi DI modülüne sahip olmalıdır.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReactionsModule {

    @Binds
    @Singleton
    abstract fun bindReactionsRepository(
        impl: ReactionsRepositoryImpl,
    ): ReactionsRepository
}