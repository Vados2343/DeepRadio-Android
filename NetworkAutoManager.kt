package com.myradio.deepradio

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.*

class NetworkAutoManager(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onNetworkChanged: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "NetworkAutoManager"
    }

    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                onNetworkChanged?.invoke(true)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")

                // Пытаемся автоматически переключиться на другую сеть
                managerScope.launch {
                    delay(2000) // Даём время на автопереключение системы
                    if (!isInternetAvailable()) {
                        tryAutoSwitchNetwork()
                    }
                }

                onNetworkChanged?.invoke(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Log.d(TAG, "Network capabilities changed. Has internet: $hasInternet")

                if (!hasInternet) {
                    managerScope.launch {
                        delay(3000)
                        tryAutoSwitchNetwork()
                    }
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        Log.d(TAG, "Network monitoring started")
    }

    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
        managerScope.cancel()
        Log.d(TAG, "Network monitoring stopped")
    }

    /**
     * Проверяет наличие интернета
     */
    fun isInternetAvailable(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking internet", e)
            false
        }
    }

    private suspend fun tryAutoSwitchNetwork() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Trying to auto-switch network...")

            try {
                val isWifiConnected = isWifiConnected()
                val isMobileDataAvailable = isMobileDataAvailable()

                when {
                    !isWifiConnected && isMobileDataAvailable -> {
                        Log.d(TAG, "WiFi lost, trying to enable mobile data")
                        enableMobileData()
                    }
                    !isMobileDataAvailable && !isWifiConnected -> {
                        Log.d(TAG, "Mobile data unavailable, trying to enable WiFi")
                        enableWifi()
                    }
                    else -> {
                        Log.d(TAG, "Network auto-switch not needed")
                    }
                }

                delay(5000)
                val internetRestored = isInternetAvailable()
                Log.d(TAG, "Internet restored: $internetRestored")

                // Вот так — если вдруг ты используешь if как выражение:
                if (internetRestored) {
                    withContext(Dispatchers.Main) {
                        onNetworkChanged?.invoke(true)
                    }
                } else {
                    // Просто ничего не делаем, или логируй
                    Log.d(TAG, "Internet NOT restored, doing nothing")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in auto network switch", e)
            }
        }
    }



    private fun isWifiConnected(): Boolean {
        return try {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isMobileDataAvailable(): Boolean {
        return try {
            telephonyManager.dataState == TelephonyManager.DATA_CONNECTED ||
                    telephonyManager.dataState == TelephonyManager.DATA_CONNECTING
        } catch (e: Exception) {
            false
        }
    }

    private fun enableWifi() {
        try {
            if (!wifiManager.isWifiEnabled) {
                // В Android 10+ нельзя программно включать WiFi
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = true
                    Log.d(TAG, "WiFi enable requested")
                } else {
                    Log.d(TAG, "Cannot enable WiFi programmatically on Android 10+")
                    // Здесь можно показать уведомление пользователю
                    showNetworkSuggestion("WiFi отключен. Включите WiFi для продолжения воспроизведения.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling WiFi", e)
        }
    }

    private fun enableMobileData() {
        try {
            // Современные Android версии не позволяют программно включать мобильные данные
            // Но можем попытаться через рефлексию для старых версий
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val telephonyService = Class.forName(telephonyManager.javaClass.name)
                    val setMobileDataEnabledMethod = telephonyService.getDeclaredMethod("setDataEnabled", Boolean::class.java)
                    setMobileDataEnabledMethod.invoke(telephonyManager, true)
                    Log.d(TAG, "Mobile data enable requested")
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot enable mobile data via reflection", e)
                }
            } else {
                Log.d(TAG, "Cannot enable mobile data programmatically on modern Android")
                showNetworkSuggestion("Нет интернета. Включите мобильные данные для продолжения воспроизведения.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling mobile data", e)
        }
    }

    private fun showNetworkSuggestion(message: String) {
        // Показываем уведомление пользователю
        NotificationHelper.showNetworkNotification(context, message)
    }

    /**
     * Получает тип текущего подключения
     */
    fun getCurrentNetworkType(): String {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}