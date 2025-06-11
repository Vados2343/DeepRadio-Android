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
import com.myradio.deepradio.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManager @Inject constructor(
    private val context: Context,
    val stationRepository: StationRepository,
    private val metadataFetcher: MetadataFetcher
) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // MediaSession для интеграции с системой
    private var mediaSession: MediaSessionCompat? = null
    private var stateBuilder: PlaybackStateCompat.Builder? = null

    // StateFlows для UI
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

    // Метаданные и настройки
    private var metadataUpdateJob: Job? = null
    private var currentStationIndex: Int = 0

    enum class ViewMode { LIST, GRID }

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val isPaused: Boolean = false
    )

    data class SongMetadata(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val genre: String = ""
    )

    init {
        initializeAudioFocus()
        initializeMediaSession()
        loadSettings()
    }

    private fun initializeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> stop()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            mediaPlayer?.setVolume(0.3f, 0.3f)
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaPlayer?.setVolume(1.0f, 1.0f)
                            if (_playbackState.value.isPaused) play()
                        }
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

    fun play(station: RadioStation? = null) {
        val targetStation = station ?: _currentStation.value ?: return

        if (station != null && station != _currentStation.value) {
            // Смена станции
            _currentStation.value = station
            _currentMetadata.value = null
            currentStationIndex = stationRepository.getAllStations().value.indexOf(station)

            // Остановка текущего плеера
            mediaPlayer?.release()
            mediaPlayer = null
        }

        requestAudioFocus()

        if (mediaPlayer == null) {
            setupMediaPlayer(targetStation)
        } else {
            mediaPlayer?.start()
        }

        _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
        updateMediaSession()
        startMetadataUpdates()
        startMediaService()
    }

    private fun setupMediaPlayer(station: RadioStation) {
        _isBuffering.value = true

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnPreparedListener {
                _isBuffering.value = false
                start()
                _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
            }

            setOnErrorListener { _, what, extra ->
                Log.e("MediaManager", "MediaPlayer error: what=$what, extra=$extra")
                _isBuffering.value = false
                _playbackState.update { it.copy(isPlaying = false, isPaused = false) }
                true
            }

            setOnBufferingUpdateListener { _, percent ->
                _isBuffering.value = percent < 100
            }

            try {
                setDataSource(station.streamUrl)
                prepareAsync()
            } catch (e: Exception) {
                Log.e("MediaManager", "Error setting data source", e)
                _isBuffering.value = false
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _playbackState.update { it.copy(isPlaying = false, isPaused = true) }
        stopMetadataUpdates()
        updateMediaSession()
    }

    fun stop() {
        stopMetadataUpdates()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        abandonAudioFocus()

        _playbackState.update { PlaybackState() }
        _currentStation.value = null
        _currentMetadata.value = null
        _isBuffering.value = false

        updateMediaSession()
        stopMediaService()
    }

    fun skipToNext() {
        val stations = stationRepository.getAllStations().value
        val currentIndex = stations.indexOf(_currentStation.value)
        if (currentIndex != -1 && stations.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % stations.size
            play(stations[nextIndex])
        }
    }

    fun skipToPrevious() {
        val stations = stationRepository.getAllStations().value
        val currentIndex = stations.indexOf(_currentStation.value)
        if (currentIndex != -1 && stations.isNotEmpty()) {
            val prevIndex = if (currentIndex == 0) stations.size - 1 else currentIndex - 1
            play(stations[prevIndex])
        }
    }

    private fun requestAudioFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        Log.d("MediaManager", "Audio focus request result: $result")
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
        val intent = Intent(context, RadioPlaybackService::class.java)
        intent.action = "com.myradio.deepradio.ACTION_REFRESH"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MediaManager", "Failed to start media service", e)
        }
    }

    private fun stopMediaService() {
        val intent = Intent(context, RadioPlaybackService::class.java)
        context.stopService(intent)
    }

    private fun updateMediaSession() {
        val station = _currentStation.value
        val metadata = _currentMetadata.value
        val playbackState = _playbackState.value

        mediaSession?.let { session ->
            // Обновляем метаданные
            if (station != null) {
                val metadataBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        metadata?.title?.takeIf { it.isNotBlank() } ?: station.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        metadata?.artist?.takeIf { it.isNotBlank() } ?: "Deep Radio")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, station.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                        metadata?.genre ?: station.categories.joinToString(", "))

                session.setMetadata(metadataBuilder.build())
            }

            // Обновляем состояние воспроизведения
            stateBuilder?.setState(
                if (playbackState.isPlaying) PlaybackStateCompat.STATE_PLAYING
                else if (playbackState.isPaused) PlaybackStateCompat.STATE_PAUSED
                else PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1f
            )
            session.setPlaybackState(stateBuilder?.build())
        }
    }

    // Метаданные
    private fun startMetadataUpdates() {
        metadataUpdateJob?.cancel()
        metadataUpdateJob = managerScope.launch {
            while (isActive) {
                _currentStation.value?.let { station ->
                    try {
                        val metadata = metadataFetcher.fetchMetadata(station)
                        _currentMetadata.value = metadata
                        updateMediaSession()
                    } catch (e: Exception) {
                        Log.e("MediaManager", "Error fetching metadata", e)
                    }
                }
                delay(10_000) // Обновляем каждые 10 секунд
            }
        }
    }

    private fun stopMetadataUpdates() {
        metadataUpdateJob?.cancel()
        metadataUpdateJob = null
    }

    // UI управление
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
        val station = _currentStation.value?.name ?: "Unknown Station"
        val metadata = _currentMetadata.value

        return if (metadata != null && metadata.title.isNotBlank()) {
            "Now playing: ${metadata.artist} - ${metadata.title} on $station"
        } else {
            "Listening to: $station"
        }
    }

    // Настройки
    private fun saveSettings() {
        val prefs = context.getSharedPreferences("radio_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("current_station", currentStationIndex)
            .putString("view_mode", _viewMode.value.name)
            .apply()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences("radio_settings", Context.MODE_PRIVATE)
        currentStationIndex = prefs.getInt("current_station", 0)

        val viewModeName = prefs.getString("view_mode", ViewMode.LIST.name)
        _viewMode.value = try {
            ViewMode.valueOf(viewModeName ?: ViewMode.LIST.name)
        } catch (e: Exception) {
            ViewMode.LIST
        }
    }

    // Методы для работы с браузером медиа (для сервиса)
    fun getMediaItems(): List<MediaBrowserCompat.MediaItem> {
        return stationRepository.getAllStations().value.map { station ->
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(station.streamUrl)
                .setTitle(station.name)
                .setSubtitle(station.categories.joinToString(", "))
                .build()

            MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }
    }

    fun getMediaSession(): MediaSessionCompat? = mediaSession

    fun release() {
        stop()
        mediaSession?.release()
        mediaSession = null
        managerScope.cancel()
    }
}