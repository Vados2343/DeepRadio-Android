package com.myradio.deepradio

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

class BluetoothAudioManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var a2dpProfile: BluetoothA2dp? = null
    private var headsetProfile: BluetoothHeadset? = null

    companion object {
        private const val TAG = "BluetoothAudioManager"
    }

    init {
        initializeProfiles()
    }

    private fun initializeProfiles() {
        if (hasBluetoothPermission()) {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    when (profile) {
                        BluetoothProfile.A2DP -> {
                            a2dpProfile = proxy as BluetoothA2dp
                            Log.d(TAG, "A2DP profile connected")
                        }
                        BluetoothProfile.HEADSET -> {
                            headsetProfile = proxy as BluetoothHeadset
                            Log.d(TAG, "Headset profile connected")
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    when (profile) {
                        BluetoothProfile.A2DP -> a2dpProfile = null
                        BluetoothProfile.HEADSET -> headsetProfile = null
                    }
                }
            }, BluetoothProfile.A2DP)

            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    headsetProfile = proxy as BluetoothHeadset
                }
                override fun onServiceDisconnected(profile: Int) {
                    headsetProfile = null
                }
            }, BluetoothProfile.HEADSET)
        }
    }

    /**
     * Проверяет, подключены ли именно АУДИО устройства Bluetooth
     */
    fun isAudioDeviceConnected(): Boolean {
        if (!hasBluetoothPermission() || bluetoothAdapter?.isEnabled != true) {
            return false
        }

        return try {
            // Проверяем A2DP (музыка)
            val a2dpConnected = a2dpProfile?.connectedDevices?.isNotEmpty() == true

            // Проверяем Headset (звонки, но многие наушники поддерживают оба профиля)
            val headsetConnected = headsetProfile?.connectedDevices?.isNotEmpty() == true

            Log.d(TAG, "Audio devices - A2DP: $a2dpConnected, Headset: $headsetConnected")

            a2dpConnected || headsetConnected
        } catch (e: SecurityException) {
            Log.e(TAG, "No Bluetooth permission", e)
            false
        }
    }

    /**
     * Получает список подключенных аудио устройств
     */
    fun getConnectedAudioDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission() || bluetoothAdapter?.isEnabled != true) {
            return emptyList()
        }

        return try {
            val devices = mutableSetOf<BluetoothDevice>()

            // Добавляем A2DP устройства
            a2dpProfile?.connectedDevices?.let { devices.addAll(it) }

            // Добавляем Headset устройства
            headsetProfile?.connectedDevices?.let { devices.addAll(it) }

            devices.toList()
        } catch (e: SecurityException) {
            Log.e(TAG, "No Bluetooth permission", e)
            emptyList()
        }
    }

    /**
     * Проверяет, должно ли воспроизведение быть только через BT
     */
    fun shouldPlayOnlyViaBluetooth(): Boolean {
        val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val onlyBluetoothEnabled = settingsPrefs.getBoolean("only_bluetooth_playback", false)

        return if (onlyBluetoothEnabled) {
            isAudioDeviceConnected()
        } else {
            true // Разрешаем воспроизведение всегда, если настройка выключена
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun release() {
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile)
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, headsetProfile)
    }
}