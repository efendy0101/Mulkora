package com.mulkora.feature.reports.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Şikâyet modülü Hilt bağımlılık modülü.
 * CLAUDE.md Bölüm 11: Şikâyet Sistemi.
 *
 * ReportViewModel, @HiltViewModel ile işaretlenmiştir.
 * FirebaseFunctions core-firebase modülünden @Singleton olarak sağlanır —
 * burada tekrar tanımlanması gerekmez.
 *
 * Bu modül gelecekte ReportRepository gibi bileşenler eklendiğinde kullanılır.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReportsModule
