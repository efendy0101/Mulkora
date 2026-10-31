package com.mulkora.feature.notifications.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mulkora.feature.notifications.domain.model.AppNotification
import com.mulkora.feature.notifications.domain.repository.NotificationsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.mulkora.feature.notifications.domain.model.NotificationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : NotificationsRepository {

    private fun notificationsRef(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    override fun getNotifications(): Flow<List<AppNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = notificationsRef(uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val notifs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationDto::class.java)?.toNotification()
                } ?: emptyList()
                trySend(notifs)
            }
        awaitClose { listener.remove() }
    }

    override fun getUnreadCount(): Flow<Int> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(0); close(); return@callbackFlow }
        val listener = notificationsRef(uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Kullanıcı oturumu yok.")
        notificationsRef(uid)
            .document(notificationId)
            .update(
                mapOf(
                    "isRead" to true,
                    "readAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            )
            .await()
    }

    override suspend fun markAllAsRead(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Kullanıcı oturumu yok.")
        val unreadSnap = notificationsRef(uid)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        if (unreadSnap.isEmpty) return@runCatching

        val batch = firestore.batch()
        val now = com.google.firebase.firestore.FieldValue.serverTimestamp()
        unreadSnap.documents.forEach { doc ->
            batch.update(doc.reference, mapOf("isRead" to true, "readAt" to now))
        }
        batch.commit().await()
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Kullanıcı oturumu yok.")
        notificationsRef(uid).document(notificationId).delete().await()
    }
}

/**
 * Firestore bildirim belgesi DTO.
 */
internal data class NotificationDto(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val deepLink: String? = null,
    val referenceId: String? = null,
    val fromUid: String? = null,
    val fromDisplayName: String? = null,
    val fromPhotoUrl: String? = null,
    val createdAt: com.google.firebase.Timestamp? = null,
) {
    fun toNotification(): AppNotification = AppNotification(
        id = id,
        userId = userId,
        type = NotificationType.fromValue(type),
        title = title,
        body = body,
        isRead = isRead,
        deepLink = deepLink,
        referenceId = referenceId,
        fromUid = fromUid,
        fromDisplayName = fromDisplayName,
        fromPhotoUrl = fromPhotoUrl,
        createdAt = createdAt,
    )
}