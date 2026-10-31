package com.mulkora.feature.reference.di

import com.mulkora.feature.reference.data.repository.ReferenceRepositoryImpl
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReferenceModule {

    @Binds
    @Singleton
    abstract fun bindReferenceRepository(
        impl: ReferenceRepositoryImpl,
    ): ReferenceRepository
}