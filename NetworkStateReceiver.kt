package com.myradio.deepradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class NetworkStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkStateReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                handleConnectivityChange(context)
            }

            "android.net.wifi.STATE_CHANGE" -> {
                handleWifiStateChange(context, intent)
            }
        }
    }

    private fun handleConnectivityChange(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        Log.d(TAG, "Connectivity changed. Has internet: $hasInternet")

        val intent = Intent("com.myradio.deepradio.NETWORK_STATE_CHANGED")
        intent.putExtra("has_internet", hasInternet)
        context.sendBroadcast(intent)
    }

    private fun handleWifiStateChange(context: Context, intent: Intent) {
        // Дополнительная обработка изменений WiFi
        Log.d(TAG, "WiFi state changed")
        handleConnectivityChange(context)
    }
}