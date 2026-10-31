package com.mulkora.feature.notifications.domain.repository

import com.mulkora.feature.notifications.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {

    /** Oturum açık kullanıcının bildirimlerini gerçek zamanlı olarak akışla döner. */
    fun getNotifications(): Flow<List<AppNotification>>

    /** Okunmamış bildirim sayısını gerçek zamanlı döner (bottom bar badge için). */
    fun getUnreadCount(): Flow<Int>

    /** Tek bir bildirimi okundu olarak işaretler. */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /** Tüm bildirimleri okundu olarak işaretler. */
    suspend fun markAllAsRead(): Result<Unit>

    /** Bildirimi siler. */
    suspend fun deleteNotification(notificationId: String): Result<Unit>
}