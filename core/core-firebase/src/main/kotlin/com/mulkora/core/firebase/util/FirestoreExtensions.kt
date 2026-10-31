package com.mulkora.core.firebase.util

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.mulkora.core.common.result.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore yardımcı extension fonksiyonları.
 * Tüm Firestore işlemleri bu katman üzerinden yapılır.
 */

/**
 * DocumentReference'ı Result<T> olarak alır.
 * Hataları yakalayıp Result.Error'a dönüştürür.
 */
suspend inline fun <reified T> DocumentReference.getAsResult(): Result<T?> {
    return try {
        val snapshot = get().await()
        if (snapshot.exists()) {
            val data = snapshot.toObject(T::class.java)
            Result.Success(data)
        } else {
            Result.Success(null)
        }
    } catch (e: Exception) {
        Result.Error(e)
    }
}

/**
 * DocumentSnapshot'tan nesneye güvenli dönüşüm.
 */
inline fun <reified T> DocumentSnapshot.toObjectOrNull(): T? =
    runCatching { toObject(T::class.java) }.getOrNull()

/**
 * Query'yi Flow olarak gözlemle (gerçek zamanlı).
 * Snapshot listener kullanır ve coroutine Flow'a dönüştürür.
 */
inline fun <reified T> Query.asFlow(): Flow<Result<List<T>>> = callbackFlow {
    val listenerRegistration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            trySend(Result.Error(error))
            return@addSnapshotListener
        }
        if (snapshot != null) {
            val items = snapshot.documents.mapNotNull { it.toObjectOrNull<T>() }
            trySend(Result.Success(items))
        }
    }
    awaitClose { listenerRegistration.remove() }
}

/**
 * DocumentReference'ı Flow olarak gözlemle (gerçek zamanlı).
 */
inline fun <reified T> DocumentReference.asFlow(): Flow<Result<T?>> = callbackFlow {
    val listenerRegistration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            trySend(Result.Error(error))
            return@addSnapshotListener
        }
        val data = snapshot?.toObjectOrNull<T>()
        trySend(Result.Success(data))
    }
    awaitClose { listenerRegistration.remove() }
}

/**
 * Query sonucunu tek seferlik alır.
 */
suspend inline fun <reified T> Query.getAsResult(): Result<List<T>> {
    return try {
        val snapshot = get().await()
        val items = snapshot.documents.mapNotNull { it.toObjectOrNull<T>() }
        Result.Success(items)
    } catch (e: Exception) {
        Result.Error(e)
    }
}