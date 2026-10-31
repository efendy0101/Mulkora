package com.mulkora.core.network.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * core-network Hilt modülü.
 *
 * Mülkora projesi Firebase Cloud Functions üzerinden backend iletişimi yaptığından
 * Retrofit/OkHttp altyapısına şu an ihtiyaç duyulmamaktadır.
 * Tüm network çağrıları Firebase SDK (FirebaseFunctions callable) ile yapılır.
 *
 * Bu modül ilerleyen sürümlerde harici API entegrasyonu gerekirse
 * Retrofit/OkHttp sağlayıcılarını içerecektir.
 *
 * CLAUDE.md §2: core-network modülü — Firebase callable functions ile çalışır.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule