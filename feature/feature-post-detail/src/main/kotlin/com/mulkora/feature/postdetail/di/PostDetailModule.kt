package com.mulkora.feature.postdetail.di

import com.mulkora.feature.postdetail.data.repository.PostDetailRepositoryImpl
import com.mulkora.feature.postdetail.domain.repository.PostDetailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostDetailModule {

    @Binds
    @Singleton
    abstract fun bindPostDetailRepository(
        impl: PostDetailRepositoryImpl,
    ): PostDetailRepository
}