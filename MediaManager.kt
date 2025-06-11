package com.myradio.deepradio.domain

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.myradio.deepradio.RadioPlaybackService
import com.myradio.deepradio.RadioStation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

// Enum для режимов воспроизведения
enum class PlaybackMode {
    INSTANT,    // Моментальное воспроизведение (для стабильного интернета)
    BUFFERED,   // Буферизованное воспроизведение (задержка 1-3 сек для нестабильного интернета)
    SMART       // Умный режим (пребуферинг соседних станций)
}

// Расширения для получения описаний режимов
fun PlaybackMode.getDisplayName(): String = when (this) {
    PlaybackMode.INSTANT -> "Мгновенный"
    PlaybackMode.BUFFERED -> "Буферизованный"
    PlaybackMode.SMART -> "Умный"
}

fun PlaybackMode.getDescription(): String = when (this) {
    PlaybackMode.INSTANT -> "Станция начинает играть сразу. Подходит для стабильного интернета."
    PlaybackMode.BUFFERED -> "Небольшая задержка (1-3 сек) для стабилизации потока. Для нестабильного интернета."
    PlaybackMode.SMART -> "Предзагружает соседние станции. Переключение без задержек, но использует больше трафика."
}

fun PlaybackMode.getRecommendation(): String = when (this) {
    PlaybackMode.INSTANT -> "Рекомендуется при Wi-Fi или стабильном 4G/5G"
    PlaybackMode.BUFFERED -> "Рекомендуется при слабом сигнале или ограниченном трафике"
    PlaybackMode.SMART -> "Рекомендуется при безлимитном интернете"
}

// ✅ КЛАСС ДЛЯ УПРАВЛЕНИЯ ПРЕДЗАГРУЖЕННЫМИ ПЛЕЕРАМИ
data class PrebufferedPlayer(
    val mediaPlayer: MediaPlayer,
    val isPrepared: Boolean,
    val stationUrl: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Singleton
class MediaManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val stationRepository: StationRepository,
    private val metadataFetcher: MetadataFetcher
) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaSession: MediaSessionCompat? = null
    private var stateBuilder: PlaybackStateCompat.Builder? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _currentMetadata = MutableStateFlow<SongMetadata?>(null)
    val currentMetadata: StateFlow<SongMetadata?> = _currentMetadata.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _expandedPlayer = MutableStateFlow(false)
    val expandedPlayer: StateFlow<Boolean> = _expandedPlayer.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    // Новое: режим воспроизведения
    private val _playbackMode = MutableStateFlow(PlaybackMode.INSTANT)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private var metadataUpdateJob: Job? = null
    private var currentStationIndex: Int = 0

    // ✅ ИСПРАВЛЕНО: Для умного режима - безопасное управление предзагруженными плеерами
    private val prebufferedPlayers = mutableMapOf<String, PrebufferedPlayer>()
    private val prebufferMutex = Mutex()
    private var prebufferJob: Job? = null

    // ✅ УЛУЧШЕНИЕ: Максимальное количество предзагруженных плееров
    private val maxPrebufferedPlayers = 3
    private val prebufferTimeoutMs = 15000L // 15 секунд на пребуферинг

    enum class ViewMode { LIST, GRID }
    data class PlaybackState(val isPlaying: Boolean = false, val isPaused: Boolean = false)
    data class SongMetadata(val title: String = "", val artist: String = "", val album: String = "", val genre: String = "")

    init {
        initializeAudioFocus()
        initializeMediaSession()
        loadSettings()

        // ✅ УЛУЧШЕНИЕ: Автоочистка старых предзагруженных плееров
        startCleanupJob()
    }

    private fun startCleanupJob() {
        managerScope.launch {
            while (isActive) {
                delay(30000) // Каждые 30 секунд
                cleanupOldPrebufferedPlayers()
            }
        }
    }

    private suspend fun cleanupOldPrebufferedPlayers() {
        prebufferMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val toRemove = prebufferedPlayers.filter { (_, player) ->
                currentTime - player.createdAt > 120000 // 2 минуты
            }

            toRemove.forEach { (url, player) ->
                try {
                    player.mediaPlayer.release()
                    prebufferedPlayers.remove(url)
                    Log.d("MediaManager", "Cleaned up old prebuffered player for: $url")
                } catch (e: Exception) {
                    Log.e("MediaManager", "Error cleaning up player for $url", e)
                }
            }
        }
    }

    private fun initializeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { change: Int ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_LOSS -> stop()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                            mediaPlayer?.setVolume(0.3f, 0.3f)
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaPlayer?.setVolume(1f, 1f)
                            if (_playbackState.value.isPaused) play()
                        }
                        else -> { /* no-op */ }
                    }
                }
                .build()
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(context, "DeepRadioSession").apply {
            stateBuilder = PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
            )
            setPlaybackState(stateBuilder!!.build())
            isActive = true
        }
    }

    // ✅ УЛУЧШЕННАЯ функция установки режима воспроизведения
    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        saveSettings()

        // Останавливаем все текущие предзагрузки
        managerScope.launch {
            clearPrebufferedPlayers()
            prebufferJob?.cancel()

            // Если включен умный режим и есть текущая станция, начинаем пребуферинг
            if (mode == PlaybackMode.SMART && _currentStation.value != null) {
                startSmartPrebuffering()
            }
        }

        Log.d("MediaManager", "Playback mode changed to: $mode")
    }

    fun play(station: RadioStation? = null) {
        val target: RadioStation = station ?: _currentStation.value ?: return

        if (station != null && station != _currentStation.value) {
            _currentStation.value = station
            _currentMetadata.value = null
            currentStationIndex = stationRepository.getAllStations().value.indexOf(station)

            // ✅ УЛУЧШЕНО: В умном режиме проверяем предзагруженные плееры
            if (_playbackMode.value == PlaybackMode.SMART) {
                managerScope.launch {
                    val prebuffered = prebufferMutex.withLock {
                        prebufferedPlayers[station.streamUrl]
                    }

                    if (prebuffered?.isPrepared == true) {
                        Log.d("MediaManager", "Using prebuffered player for ${station.name}")

                        // Останавливаем текущий плеер
                        mediaPlayer?.release()

                        // Используем предзагруженный
                        mediaPlayer = prebuffered.mediaPlayer
                        prebufferMutex.withLock {
                            prebufferedPlayers.remove(station.streamUrl)
                        }

                        requestAudioFocus()
                        mediaPlayer?.start()
                        _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
                        updateMediaSession()
                        startMetadataUpdates()
                        startMediaService()

                        // Запускаем пребуферинг новых соседних станций
                        startSmartPrebuffering()
                        return@launch
                    }
                }
            }

            mediaPlayer?.release()
            mediaPlayer = null
        }

        requestAudioFocus()
        if (mediaPlayer == null) setupMediaPlayer(target) else mediaPlayer?.start()
        _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
        updateMediaSession()
        startMetadataUpdates()
        startMediaService()

        // Для умного режима запускаем пребуферинг после начала воспроизведения
        if (_playbackMode.value == PlaybackMode.SMART) {
            managerScope.launch {
                delay(2000) // Даем время основному плееру стабилизироваться
                startSmartPrebuffering()
            }
        }
    }

    private fun setupMediaPlayer(station: RadioStation) {
        _isBuffering.value = true

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnPreparedListener {
                _isBuffering.value = false

                // Применяем задержку в зависимости от режима
                when (_playbackMode.value) {
                    PlaybackMode.INSTANT -> {
                        start()
                        _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
                    }
                    PlaybackMode.BUFFERED -> {
                        // Задержка 1-3 секунды для стабилизации
                        managerScope.launch {
                            delay(2000) // 2 секунды буферизации
                            if (!isActive) return@launch
                            try {
                                start()
                                _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
                            } catch (e: Exception) {
                                Log.e("MediaManager", "Error starting buffered playback", e)
                            }
                        }
                    }
                    PlaybackMode.SMART -> {
                        start()
                        _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
                    }
                }
            }

            setOnErrorListener { _, what: Int, extra: Int ->
                Log.e("MediaManager", "Error: what=$what extra=$extra")
                _isBuffering.value = false
                _playbackState.value = PlaybackState()
                true
            }

            setOnBufferingUpdateListener { _, percent: Int ->
                _isBuffering.value = percent < 100
            }

            try {
                setDataSource(station.streamUrl)
                prepareAsync()
            } catch (e: Exception) {
                Log.e("MediaManager", "DataSource error", e)
                _isBuffering.value = false
            }
        }
    }

    // ✅ УЛУЧШЕННАЯ функция умного пребуферинга
    private fun startSmartPrebuffering() {
        prebufferJob?.cancel()
        prebufferJob = managerScope.launch {
            try {
                delay(3000) // Ждем 3 секунды после начала воспроизведения

                val currentStation = _currentStation.value ?: return@launch
                val allStations = stationRepository.getAllStations().value
                val currentIndex = allStations.indexOf(currentStation)

                if (currentIndex == -1) return@launch

                // Определяем соседние станции
                val nextStation = if (currentIndex < allStations.size - 1) allStations[currentIndex + 1] else allStations[0]
                val prevStation = if (currentIndex > 0) allStations[currentIndex - 1] else allStations.last()

                Log.d("MediaManager", "Starting smart prebuffering for: ${nextStation.name} and ${prevStation.name}")

                // Проверяем, не превышаем ли лимит
                val currentCount = prebufferMutex.withLock { prebufferedPlayers.size }

                // Пребуферинг следующей станции
                if (currentCount < maxPrebufferedPlayers) {
                    val hasNext = prebufferMutex.withLock {
                        prebufferedPlayers.containsKey(nextStation.streamUrl)
                    }
                    if (!hasNext) {
                        prebufferStation(nextStation)
                        delay(1000) // Задержка между пребуферингами
                    }
                }

                // Пребуферинг предыдущей станции
                val currentCountAfter = prebufferMutex.withLock { prebufferedPlayers.size }
                if (currentCountAfter < maxPrebufferedPlayers) {
                    val hasPrev = prebufferMutex.withLock {
                        prebufferedPlayers.containsKey(prevStation.streamUrl)
                    }
                    if (!hasPrev) {
                        prebufferStation(prevStation)
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaManager", "Error in smart prebuffering", e)
            }
        }
    }

    // ✅ ИСПРАВЛЕННАЯ функция пребуферинга станции
    private suspend fun prebufferStation(station: RadioStation) = withContext(Dispatchers.IO) {
        try {
            Log.d("MediaManager", "Starting prebuffer for: ${station.name}")

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            }

            // ✅ ИСПРАВЛЕНО: Используем CompletableDeferred для ожидания подготовки
            val preparationResult = CompletableDeferred<Boolean>()

            player.setOnPreparedListener {
                Log.d("MediaManager", "Prebuffered successfully: ${station.name}")
                preparationResult.complete(true)
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("MediaManager", "Prebuffer error for ${station.name}: what=$what extra=$extra")
                preparationResult.complete(false)
                true
            }

            try {
                player.setDataSource(station.streamUrl)
                player.prepareAsync()

                // Ждем результат с таймаутом
                val isSuccess = withTimeoutOrNull(prebufferTimeoutMs) {
                    preparationResult.await()
                } ?: false

                if (isSuccess) {
                    val prebufferedPlayer = PrebufferedPlayer(
                        mediaPlayer = player,
                        isPrepared = true,
                        stationUrl = station.streamUrl
                    )

                    prebufferMutex.withLock {
                        // Проверяем лимит перед добавлением
                        if (prebufferedPlayers.size < maxPrebufferedPlayers) {
                            prebufferedPlayers[station.streamUrl] = prebufferedPlayer
                            Log.d("MediaManager", "Successfully stored prebuffered player for: ${station.name}")
                        } else {
                            // Если превышен лимит, освобождаем ресурсы
                            player.release()
                            Log.w("MediaManager", "Prebuffer limit reached, discarding player for: ${station.name}")
                        }
                    }
                } else {
                    player.release()
                    Log.w("MediaManager", "Failed to prebuffer: ${station.name}")
                }
            } catch (e: Exception) {
                player.release()
                Log.e("MediaManager", "Exception while setting up prebuffer for ${station.name}", e)
            }
        } catch (e: Exception) {
            Log.e("MediaManager", "Exception in prebufferStation for ${station.name}", e)
        }
    }

    // ✅ УЛУЧШЕННАЯ функция очистки предзагруженных плееров
    private suspend fun clearPrebufferedPlayers() {
        prebufferMutex.withLock {
            prebufferedPlayers.values.forEach { prebufferedPlayer ->
                try {
                    prebufferedPlayer.mediaPlayer.release()
                } catch (e: Exception) {
                    Log.e("MediaManager", "Error releasing prebuffered player", e)
                }
            }
            prebufferedPlayers.clear()
            Log.d("MediaManager", "Cleared all prebuffered players")
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _playbackState.update { it.copy(isPlaying = false, isPaused = true) }
        stopMetadataUpdates()
        updateMediaSession()

        // В умном режиме не останавливаем пребуферинг при паузе
        if (_playbackMode.value != PlaybackMode.SMART) {
            managerScope.launch {
                clearPrebufferedPlayers()
            }
        }
    }

    fun stop() {
        stopMetadataUpdates()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
        _playbackState.value = PlaybackState()
        _currentStation.value = null
        _currentMetadata.value = null
        _isBuffering.value = false
        updateMediaSession()
        stopMediaService()

        // Очищаем пребуферинг
        managerScope.launch {
            clearPrebufferedPlayers()
            prebufferJob?.cancel()
        }
    }

    fun skipToNext() {
        val list = stationRepository.getAllStations().value
        val idx = list.indexOf(_currentStation.value)
        if (idx >= 0 && list.isNotEmpty()) play(list[(idx + 1) % list.size])
    }

    fun skipToPrevious() {
        val list = stationRepository.getAllStations().value
        val idx = list.indexOf(_currentStation.value)
        if (idx >= 0 && list.isNotEmpty()) {
            play(list[if (idx == 0) list.lastIndex else idx - 1])
        }
    }

    private fun requestAudioFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        Log.d("MediaManager", "Audio focus result: $result")
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun startMediaService() {
        Intent(context, RadioPlaybackService::class.java).also { intent ->
            intent.action = "com.myradio.deepradio.ACTION_REFRESH"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun stopMediaService() {
        context.stopService(Intent(context, RadioPlaybackService::class.java))
    }

    private fun updateMediaSession() {
        mediaSession?.apply {
            _currentStation.value?.let { station ->
                val md = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        _currentMetadata.value?.title?.takeIf { it.isNotBlank() } ?: station.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        _currentMetadata.value?.artist?.takeIf { it.isNotBlank() } ?: "Deep Radio")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, station.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                        _currentMetadata.value?.genre ?: station.categories.joinToString(", "))
                    .build()
                setMetadata(md)
            }
            stateBuilder?.setState(
                when {
                    _playbackState.value.isPlaying -> PlaybackStateCompat.STATE_PLAYING
                    _playbackState.value.isPaused  -> PlaybackStateCompat.STATE_PAUSED
                    else                            -> PlaybackStateCompat.STATE_STOPPED
                },
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1f
            )
            setPlaybackState(stateBuilder!!.build())
        }
    }

    private fun startMetadataUpdates() {
        metadataUpdateJob?.cancel()
        metadataUpdateJob = managerScope.launch {
            while (isActive) {
                _currentStation.value?.let { station ->
                    try {
                        val md = metadataFetcher.fetchMetadata(station)
                        _currentMetadata.value = md
                        updateMediaSession()
                    } catch (e: Exception) {
                        Log.e("MediaManager", "Meta fetch error", e)
                    }
                }
                delay(10_000)
            }
        }
    }

    private fun stopMetadataUpdates() {
        metadataUpdateJob?.cancel()
        metadataUpdateJob = null
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.LIST -> ViewMode.GRID
            ViewMode.GRID -> ViewMode.LIST
        }
        saveSettings()
    }

    fun setExpandedPlayer(expanded: Boolean) {
        _expandedPlayer.value = expanded
    }

    fun getCurrentSongInfo(): String {
        val name = _currentStation.value?.name ?: "Unknown Station"
        val md   = _currentMetadata.value
        return if (md != null && md.title.isNotBlank()) {
            "Now playing: ${md.artist} — ${md.title} on $name"
        } else {
            "Listening to: $name"
        }
    }

    // ✅ УЛУЧШЕННАЯ функция получения статистики
    fun getPrebufferStats(): String {
        return "Prebuffered: ${prebufferedPlayers.size}/$maxPrebufferedPlayers players"
    }

    private fun saveSettings() {
        context.getSharedPreferences("radio_settings", Context.MODE_PRIVATE).edit().apply {
            putInt("current_station", currentStationIndex)
            putString("view_mode", _viewMode.value.name)
            putString("playback_mode", _playbackMode.value.name)
            apply()
        }
    }

    private fun loadSettings() {
        context.getSharedPreferences("radio_settings", Context.MODE_PRIVATE).run {
            currentStationIndex = getInt("current_station", 0)
            _viewMode.value = try {
                ViewMode.valueOf(getString("view_mode", ViewMode.LIST.name)!!)
            } catch (e: Exception) {
                ViewMode.LIST
            }
            _playbackMode.value = try {
                PlaybackMode.valueOf(getString("playback_mode", PlaybackMode.INSTANT.name)!!)
            } catch (e: Exception) {
                PlaybackMode.INSTANT
            }
        }
    }

    fun getMediaItems(): List<MediaBrowserCompat.MediaItem> =
        stationRepository.getAllStations().value.map { station ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(station.streamUrl)
                    .setTitle(station.name)
                    .setSubtitle(station.categories.joinToString(", "))
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }

    fun getMediaSession(): MediaSessionCompat? = mediaSession

    fun release() {
        stop()
        managerScope.launch {
            clearPrebufferedPlayers()
            prebufferJob?.cancel()
        }
        mediaSession?.release()
        managerScope.cancel()
    }
}