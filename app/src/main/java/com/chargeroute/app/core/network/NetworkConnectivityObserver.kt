package com.chargeroute.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Consulta puntual: ¿hay internet en este momento?
    val isCurrentlyConnected: Boolean
        get() {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return hasInternet(capabilities)
        }

    /**
     * Avisa cada vez que cambia la conexion: true = hay internet, false = sin conexion.
     * Se escucha la red por defecto del telefono, asi pasar de Wi-Fi a datos moviles
     * no se toma como una perdida de conexion.
     */
    fun observeIsOnline(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(hasInternet(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        // Valor inicial y despues cada cambio
        trySend(isCurrentlyConnected)
        connectivityManager.registerDefaultNetworkCallback(callback)

        // Cuando nadie escucha mas (por ejemplo la app se cierra) se deja de observar
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun hasInternet(capabilities: NetworkCapabilities): Boolean =
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
