package com.mulkora.feature.profile.di

import com.mulkora.feature.profile.data.repository.ProfileRepositoryImpl
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl,
    ): ProfileRepository
}