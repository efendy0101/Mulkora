package com.mulkora.feature.notifications.di

import com.mulkora.feature.notifications.data.repository.NotificationsRepositoryImpl
import com.mulkora.feature.notifications.domain.repository.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(
        impl: NotificationsRepositoryImpl,
    ): NotificationsRepository
}