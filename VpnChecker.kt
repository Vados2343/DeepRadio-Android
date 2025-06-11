package com.myradio.deepradio

import android.content.Context
import android.net.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VpnChecker(
    private val context: Context,
    private val onVpnDetected: () -> Unit
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkVpnAsync()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = checkVpnAsync()
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            try {
                connectivityManager.registerNetworkCallback(req, networkCallback)
            } catch (e: Exception) {
                Log.e("VpnChecker", "registerNetworkCallback failed", e)
            }
        }
        // И проверим сразу при старте
        checkVpnAsync()
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                Log.e("VpnChecker", "unregisterNetworkCallback failed", e)
            }
        }
    }

    private fun checkVpnAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            val vpn = isVpnActive()
            val proxy = isProxyEnabled()
            if (vpn || proxy) {
                withContext(Dispatchers.Main) {
                    onVpnDetected()
                }
            }
        }
    }

    private fun isVpnActive(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(net) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else {
            @Suppress("DEPRECATION")
            return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)
                ?.isConnected == true
        }
    }

    private fun isProxyEnabled(): Boolean {
        val host = System.getProperty("http.proxyHost")
        val port = System.getProperty("http.proxyPort")
        return !host.isNullOrEmpty() && !port.isNullOrEmpty()
    }
}
