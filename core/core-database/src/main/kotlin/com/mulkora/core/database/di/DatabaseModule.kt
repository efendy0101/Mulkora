package com.mulkora.core.database.di

import com.mulkora.core.database.datastore.UserPreferencesDataStore
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * core-database Hilt modülü.
 *
 * [UserPreferencesDataStore], [Singleton] ve [@Inject constructor] ile
 * işaretlendiğinden Hilt tarafından otomatik sağlanır.
 * Bu modül gelecekte Room veritabanı ve DAO'ları için kullanılacaktır.
 *
 * CLAUDE.md §2: DataStore, Room — core-database modülünde saklanır.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule