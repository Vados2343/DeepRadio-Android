package com.myradio.deepradio.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myradio.deepradio.domain.MediaManager
import com.myradio.deepradio.domain.PlaybackMode
import com.myradio.deepradio.presentation.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaManager: MediaManager
) : ViewModel() {

    private val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Основные настройки воспроизведения
    private val backgroundPlaybackEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("background_playback", true)
    )

    private val onlyBluetoothPlaybackEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("only_bluetooth_playback", false)
    )

    private val autoRotateEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("auto_rotate", true)
    )

    // Управление темой приложения
    private val selectedTheme = MutableStateFlow(
        try {
            AppTheme.valueOf(
                settingsPrefs.getString("app_theme", AppTheme.DARK.name) ?: AppTheme.DARK.name
            )
        } catch (e: Exception) {
            AppTheme.DARK
        }
    )

    // Управление режимом воспроизведения
    private val selectedPlaybackMode = MutableStateFlow(
        try {
            PlaybackMode.valueOf(
                settingsPrefs.getString("playback_mode", PlaybackMode.INSTANT.name) ?: PlaybackMode.INSTANT.name
            )
        } catch (e: Exception) {
            PlaybackMode.INSTANT
        }
    )

    // Дополнительные настройки
    private val notificationsEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("notifications_enabled", true)
    )

    private val showLyrics = MutableStateFlow(
        settingsPrefs.getBoolean("show_lyrics", true)
    )

    private val autoPlay = MutableStateFlow(
        settingsPrefs.getBoolean("auto_play", false)
    )

    // Объединение всех состояний в одно UI состояние
    private val basicSettings = combine(
        backgroundPlaybackEnabled,
        onlyBluetoothPlaybackEnabled,
        autoRotateEnabled,
        selectedTheme,
        selectedPlaybackMode
    ) { background, bluetooth, rotate, theme, playbackMode ->
        BasicSettings(background, bluetooth, rotate, theme, playbackMode)
    }

    private val additionalSettings = combine(
        notificationsEnabled,
        showLyrics,
        autoPlay
    ) { notifications, lyrics, autoPlayValue ->
        AdditionalSettings(notifications, lyrics, autoPlayValue)
    }

    val uiState = combine(
        basicSettings,
        additionalSettings
    ) { basic, additional ->
        SettingsUiState(
            backgroundPlaybackEnabled = basic.backgroundPlayback,
            onlyBluetoothPlaybackEnabled = basic.onlyBluetoothPlayback,
            autoRotateEnabled = basic.autoRotate,
            selectedTheme = basic.theme,
            selectedPlaybackMode = basic.playbackMode,
            notificationsEnabled = additional.notifications,
            showLyrics = additional.showLyrics,
            autoPlay = additional.autoPlay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        // Инициализация: синхронизируем режим воспроизведения с MediaManager
        viewModelScope.launch {
            val savedMode = selectedPlaybackMode.value
            mediaManager.setPlaybackMode(savedMode)
        }
    }

    // Функции для изменения настроек
    fun setBackgroundPlayback(enabled: Boolean) {
        backgroundPlaybackEnabled.value = enabled
        saveToPrefs("background_playback", enabled)
    }

    fun setOnlyBluetoothPlayback(enabled: Boolean) {
        onlyBluetoothPlaybackEnabled.value = enabled
        saveToPrefs("only_bluetooth_playback", enabled)
    }

    fun setAutoRotate(enabled: Boolean) {
        autoRotateEnabled.value = enabled
        saveToPrefs("auto_rotate", enabled)

        viewModelScope.launch {
            val intent = android.content.Intent("com.myradio.deepradio.AUTO_ROTATE_CHANGED")
            intent.putExtra("enabled", enabled)
            context.sendBroadcast(intent)
        }
    }

    fun setTheme(theme: AppTheme) {
        selectedTheme.value = theme
        saveToPrefs("app_theme", theme.name)

        // Отправляем broadcast для обновления темы во всем приложении
        viewModelScope.launch {
            val intent = android.content.Intent("com.myradio.deepradio.THEME_CHANGED")
            intent.putExtra("theme", theme.name)
            context.sendBroadcast(intent)
        }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        selectedPlaybackMode.value = mode
        saveToPrefs("playback_mode", mode.name)

        // Синхронизируем с MediaManager
        viewModelScope.launch {
            mediaManager.setPlaybackMode(mode)
        }

        // Отправляем broadcast для других компонентов
        viewModelScope.launch {
            val intent = android.content.Intent("com.myradio.deepradio.PLAYBACK_MODE_CHANGED")
            intent.putExtra("mode", mode.name)
            context.sendBroadcast(intent)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        saveToPrefs("notifications_enabled", enabled)
    }

    fun setShowLyrics(enabled: Boolean) {
        showLyrics.value = enabled
        saveToPrefs("show_lyrics", enabled)
    }

    fun setAutoPlay(enabled: Boolean) {
        autoPlay.value = enabled
        saveToPrefs("auto_play", enabled)
    }

    // Сброс всех настроек к значениям по умолчанию
    fun resetToDefaults() {
        viewModelScope.launch {
            setBackgroundPlayback(true)
            setOnlyBluetoothPlayback(false)
            setAutoRotate(true)
            setTheme(AppTheme.DARK)
            setPlaybackMode(PlaybackMode.INSTANT)
            setNotificationsEnabled(true)
            setShowLyrics(true)
            setAutoPlay(false)
        }
    }

    // Получение информации о хранилище
    fun getStorageInfo(): StorageInfo {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val radioPrefs = context.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)

        return StorageInfo(
            settingsSize = prefs.all.size,
            favoritesCount = radioPrefs.getStringSet("favorites", emptySet())?.size ?: 0,
            cacheSize = getCacheSize()
        )
    }

    private fun getCacheSize(): String {
        val cacheDir = context.cacheDir
        val size = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }
    }

    // Очистка кэша
    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                // Отправляем уведомление об успешной очистке
                val intent = android.content.Intent("com.myradio.deepradio.CACHE_CLEARED")
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                // Логируем ошибку
                android.util.Log.e("SettingsViewModel", "Error clearing cache", e)
            }
        }
    }

    // Вспомогательные функции для сохранения настроек
    private fun saveToPrefs(key: String, value: Boolean) {
        settingsPrefs.edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun saveToPrefs(key: String, value: String) {
        settingsPrefs.edit()
            .putString(key, value)
            .apply()
    }
}

// Расширенное UI состояние
data class SettingsUiState(
    val backgroundPlaybackEnabled: Boolean = true,
    val onlyBluetoothPlaybackEnabled: Boolean = false,
    val autoRotateEnabled: Boolean = true,
    val selectedTheme: AppTheme = AppTheme.DARK,
    val selectedPlaybackMode: PlaybackMode = PlaybackMode.INSTANT,
    val notificationsEnabled: Boolean = true,
    val showLyrics: Boolean = true,
    val autoPlay: Boolean = false
)

// Информация о хранилище
data class StorageInfo(
    val settingsSize: Int,
    val favoritesCount: Int,
    val cacheSize: String
)

// Вспомогательные классы для combine
private data class BasicSettings(
    val backgroundPlayback: Boolean,
    val onlyBluetoothPlayback: Boolean,
    val autoRotate: Boolean,
    val theme: AppTheme,
    val playbackMode: PlaybackMode
)

private data class AdditionalSettings(
    val notifications: Boolean,
    val showLyrics: Boolean,
    val autoPlay: Boolean
)