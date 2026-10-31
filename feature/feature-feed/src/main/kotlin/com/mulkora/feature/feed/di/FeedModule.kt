package com.mulkora.feature.feed.di

import com.mulkora.feature.feed.data.repository.FeedRepositoryImpl
import com.mulkora.feature.feed.domain.repository.FeedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Feed modülü Hilt bağımlılık modülü.
 *
 * NOT: Yorumlar artık :feature-comments modülünde yönetilmektedir.
 * CommentRepository ve CommentsViewModel feature-feed'den kaldırıldı.
 * Bkz: NavGraphs.kt — CommentsScreen import'u feature-comments paketinden geliyor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeedModule {

    @Binds
    @Singleton
    abstract fun bindFeedRepository(
        impl: FeedRepositoryImpl,
    ): FeedRepository
}
