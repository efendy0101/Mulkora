package com.mulkora.core.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Ağ bağlantısı durumunu gözlemler.
 *
 * docs/22-development-phases.md Faz 13: NetworkConnectivityObserver.
 * CLAUDE.md §33: İnternet kesintisi durumunu yönetir.
 *
 * Kullanım:
 * ```kotlin
 * val observer = NetworkConnectivityObserver(context)
 * observer.observe().collectAsStateWithLifecycle()
 * ```
 */
class NetworkConnectivityObserver(
    context: Context,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Ağ bağlantısı durumunu [NetworkStatus] olarak yayımlar.
     * distinctUntilChanged ile gereksiz güncellemeler bastırılır.
     */
    fun observe(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Lost)
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Mevcut durumu ilk değer olarak yayımla
        val currentStatus = if (isConnected()) NetworkStatus.Available else NetworkStatus.Lost
        trySend(currentStatus)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * Ağ bağlantısı durumu.
 */
sealed interface NetworkStatus {
    /** Ağ bağlantısı mevcut ve internet erişimi var. */
    data object Available : NetworkStatus

    /** Ağ bağlantısı kesildi. */
    data object Lost : NetworkStatus

    /** Ağ bağlantısı hiç yok. */
    data object Unavailable : NetworkStatus
}