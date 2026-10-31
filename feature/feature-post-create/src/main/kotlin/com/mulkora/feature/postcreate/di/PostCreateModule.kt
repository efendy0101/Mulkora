package com.mulkora.feature.postcreate.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * PostCreate feature DI modülü.
 *
 * CLAUDE.md §Mimari: Her feature kendi DI modülünü sağlar.
 *
 * CreatePostViewModel'in bağımlılıkları:
 *   - FeedRepository    → feature-feed modülündeki FeedModule (@Binds)
 *   - ImageUploadRepository → core-firebase modülündeki FirebaseModule (@Singleton, @Inject constructor)
 *
 * Bu bağımlılıklar kendi modüllerinde zaten tanımlanmıştır;
 * burada tekrar binding gerekmez.
 *
 * Gelecekte PostCreateRepository gibi özel bir veri katmanı eklenirse
 * @Binds buraya eklenir.
 */
@Module
@InstallIn(SingletonComponent::class)
object PostCreateModule