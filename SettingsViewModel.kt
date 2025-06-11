package com.myradio.deepradio.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _backgroundPlaybackEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("background_playback", true)
    )

    private val _onlyBluetoothPlaybackEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("only_bluetooth_playback", false)
    )

    private val _autoRotateEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("auto_rotate", true)
    )

    val uiState = combine(
        _backgroundPlaybackEnabled,
        _onlyBluetoothPlaybackEnabled,
        _autoRotateEnabled
    ) { background, bluetooth, rotate ->
        SettingsUiState(
            backgroundPlaybackEnabled = background,
            onlyBluetoothPlaybackEnabled = bluetooth,
            autoRotateEnabled = rotate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setBackgroundPlayback(enabled: Boolean) {
        _backgroundPlaybackEnabled.value = enabled
        saveToPrefs("background_playback", enabled)
    }

    fun setOnlyBluetoothPlayback(enabled: Boolean) {
        _onlyBluetoothPlaybackEnabled.value = enabled
        saveToPrefs("only_bluetooth_playback", enabled)
    }

    fun setAutoRotate(enabled: Boolean) {
        _autoRotateEnabled.value = enabled
        saveToPrefs("auto_rotate", enabled)

        // Уведомляем MainActivity об изменении
        viewModelScope.launch {
            val intent = android.content.Intent("com.myradio.deepradio.AUTO_ROTATE_CHANGED")
            intent.putExtra("enabled", enabled)
            context.sendBroadcast(intent)
        }
    }

    private fun saveToPrefs(key: String, value: Boolean) {
        settingsPrefs.edit()
            .putBoolean(key, value)
            .apply()
    }
}

data class SettingsUiState(
    val backgroundPlaybackEnabled: Boolean = true,
    val onlyBluetoothPlaybackEnabled: Boolean = false,
    val autoRotateEnabled: Boolean = true
)