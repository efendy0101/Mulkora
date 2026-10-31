package com.mulkora.feature.comments.di

import com.mulkora.feature.comments.data.repository.CommentsRepositoryImpl
import com.mulkora.feature.comments.domain.repository.CommentsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Yorumlar modülü Hilt bağımlılık modülü.
 * CLAUDE.md §9: Yorum ve Cevap Sistemi.
 *
 * CommentsRepository arayüzünü CommentsRepositoryImpl ile bağlar.
 * CommentsViewModel bu arayüzü constructor injection ile alır.
 *
 * Yazma işlemleri (addComment/deleteComment/toggleCommentLike) Cloud Functions
 * üzerinden yapılır — doğrudan Firestore yazımı yasak (firestore.rules §comments).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CommentsModule {

    @Binds
    @Singleton
    abstract fun bindCommentsRepository(
        impl: CommentsRepositoryImpl,
    ): CommentsRepository
}