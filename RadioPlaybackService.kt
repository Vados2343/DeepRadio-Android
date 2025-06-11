package com.myradio.deepradio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.myradio.deepradio.domain.MediaManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@AndroidEntryPoint
class RadioPlaybackService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var mediaManager: MediaManager

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val updateMetadataRunnable = object : Runnable {
        override fun run() {
            updateMediaSessionMetadata()
            showNotification()
            handler.postDelayed(this, 10_000L)
        }
    }

    companion object {
        private const val TAG = "RadioPlaybackService"
        private const val CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_REFRESH = "com.myradio.deepradio.ACTION_REFRESH_NOTIFICATION"
        private const val ROOT_ID = "media_root_id"
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                onPauseRequested()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        initMediaSession()
        createChannel()
        initAudioFocus()

        // Подписываемся на изменения состояния воспроизведения
        observePlaybackState()

        handler.post(updateMetadataRunnable)
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))

        // Запускаем сервис в foreground сразу
        startForeground(NOTIFICATION_ID, buildNotification())

        Log.d(TAG, "Service created successfully")
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            combine(
                mediaManager.playbackState,
                mediaManager.currentStation,
                mediaManager.currentMetadata
            ) { playbackState, station, metadata ->
                Triple(playbackState, station, metadata)
            }.collect { (playbackState, station, metadata) ->
                updatePlaybackState(playbackState.isPlaying)
                showNotification()
            }
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "RadioService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = onPlayRequested()
                override fun onPause() = onPauseRequested()
                override fun onSkipToNext() {
                    onSkipNext()
                    onPlayRequested()
                }
                override fun onSkipToPrevious() {
                    onSkipPrev()
                    onPlayRequested()
                }
                override fun onStop() = onStopRequested()
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    // Здесь можно добавить логику воспроизведения по ID станции
                    onPlayRequested()
                }
            })

            stateBuilder = PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
            )
            setPlaybackState(stateBuilder.build())
            isActive = true
            setSessionToken(sessionToken)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Плеер Deep Radio"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focus ->
                    when (focus) {
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS -> onPauseRequested()
                        AudioManager.AUDIOFOCUS_GAIN -> onPlayRequested()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            // Уменьшаем громкость, но не ставим на паузу
                            Log.d(TAG, "Audio focus: ducking")
                        }
                    }
                }
                .build()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> {
                showNotification()
            }
            else -> {
                // Все остальные события (Media Button, Play/Pause/Next/Prev)
                // передаём в стандартный обработчик
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }
        return START_STICKY
    }

    private fun onPlayRequested() {
        Log.d(TAG, "Play requested")
        requestAudioFocus()
        mediaManager.play()
        mediaSession.isActive = true
    }

    private fun onPauseRequested() {
        Log.d(TAG, "Pause requested")
        mediaManager.pause()
    }

    private fun onSkipNext() {
        Log.d(TAG, "Skip next requested")
        mediaManager.skipToNext()
    }

    private fun onSkipPrev() {
        Log.d(TAG, "Skip previous requested")
        mediaManager.skipToPrevious()
    }

    private fun onStopRequested() {
        Log.d(TAG, "Stop requested")
        mediaManager.stop()
        abandonAudioFocus()
        mediaSession.isActive = false
        stopForeground(true)
        stopSelf()
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

        Log.d(TAG, "Audio focus request result: $result")
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        stateBuilder.setState(
            state,
            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            1f
        )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaSessionMetadata() {
        serviceScope.launch {
            val station = mediaManager.currentStation.value
            val metadata = mediaManager.currentMetadata.value

            if (station != null) {
                val metaBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        metadata?.title?.takeIf { it.isNotBlank() } ?: station.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        metadata?.artist?.takeIf { it.isNotBlank() } ?: "")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, station.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                        metadata?.genre ?: station.categories.joinToString(", "))

                try {
                    val bitmap = BitmapFactory.decodeResource(resources, station.iconResId)
                    metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load station icon", e)
                }

                mediaSession.setMetadata(metaBuilder.build())
            }
        }
    }

    private fun buildNotification(): Notification {
        val station = mediaManager.currentStation.value
        val metadata = mediaManager.currentMetadata.value
        val isPlaying = mediaManager.playbackState.value.isPlaying

        if (station == null) {
            return createEmptyNotification()
        }

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.pausev, "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.playb, "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
            )
        }

        val title = metadata?.title?.takeIf { it.isNotBlank() } ?: station.name
        val artist = metadata?.artist?.takeIf { it.isNotBlank() } ?: "Deep Radio"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(station.name)
            .setSmallIcon(R.drawable.logo)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.perv, "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                )
            )
            .addAction(playPauseAction)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.next, "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .apply {
                try {
                    val bitmap = BitmapFactory.decodeResource(resources, station.iconResId)
                    setLargeIcon(bitmap)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set large icon", e)
                }
            }
            .build()
    }

    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Deep Radio")
            .setContentText("Ready to play")
            .setSmallIcon(R.drawable.logo)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    private fun showNotification() {
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying")

        serviceScope.cancel()
        handler.removeCallbacks(updateMetadataRunnable)

        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister bluetooth receiver", e)
        }

        abandonAudioFocus()
        mediaSession.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != ROOT_ID) {
            result.sendResult(null)
            return
        }

        serviceScope.launch {
            try {
                val stations = mediaManager.stationRepository.getAllStations().value
                val items = stations.map { station ->
                    val desc = MediaDescriptionCompat.Builder()
                        .setMediaId(station.streamUrl)
                        .setTitle(station.name)
                        .setSubtitle(station.categories.joinToString(", "))
                        .build()
                    MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                }
                result.sendResult(items.toMutableList())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load children", e)
                result.sendResult(mutableListOf())
            }
        }
    }
}