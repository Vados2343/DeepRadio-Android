package com.myradio.deepradio

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothStateReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Bluetooth device connected: ${device?.name}")
                handleBluetoothDeviceConnected(context, device)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Bluetooth device disconnected: ${device?.name}")
                handleBluetoothDeviceDisconnected(context, device)
            }

            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED",
            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> {
                val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1)
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Audio profile state changed: $state for device: ${device?.name}")
                handleAudioProfileStateChanged(context, state, device)
            }
        }
    }

    private fun handleBluetoothDeviceConnected(context: Context, device: BluetoothDevice?) {
        // Проверяем настройки и при необходимости возобновляем воспроизведение
        val bluetoothManager = BluetoothAudioManager(context)
        if (bluetoothManager.shouldPlayOnlyViaBluetooth()) {
            // Возобновляем воспроизведение если было приостановлено
            val intent = Intent("com.myradio.deepradio.BLUETOOTH_AUDIO_CONNECTED")
            context.sendBroadcast(intent)
        }
    }

    private fun handleBluetoothDeviceDisconnected(context: Context, device: BluetoothDevice?) {
        val bluetoothManager = BluetoothAudioManager(context)

        // Если включена настройка "только через BT" и больше нет аудио устройств
        if (!bluetoothManager.shouldPlayOnlyViaBluetooth()) {
            // Приостанавливаем воспроизведение
            val intent = Intent("com.myradio.deepradio.BLUETOOTH_AUDIO_DISCONNECTED")
            context.sendBroadcast(intent)
        }
    }

    private fun handleAudioProfileStateChanged(context: Context, state: Int, device: BluetoothDevice?) {
        val bluetoothManager = BluetoothAudioManager(context)

        when (state) {
            2 -> { // CONNECTED
                if (bluetoothManager.shouldPlayOnlyViaBluetooth()) {
                    val intent = Intent("com.myradio.deepradio.BLUETOOTH_AUDIO_CONNECTED")
                    context.sendBroadcast(intent)
                }
            }
            0 -> { // DISCONNECTED
                if (!bluetoothManager.shouldPlayOnlyViaBluetooth()) {
                    val intent = Intent("com.myradio.deepradio.BLUETOOTH_AUDIO_DISCONNECTED")
                    context.sendBroadcast(intent)
                }
            }
        }
    }
}