package com.myradio.deepradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.annotation.Keep
import com.myradio.deepradio.domain.MediaManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@Keep
@AndroidEntryPoint
class VoiceActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "VoiceActionReceiver"
    }

    @Inject
    lateinit var mediaManager: MediaManager

    private val receiverScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            "com.google.android.gms.actions.SEARCH_ACTION" -> {
                handleSearchAction(context, intent)
            }
            "android.intent.action.VOICE_COMMAND" -> {
                handleVoiceCommand(context, intent)
            }
            "com.google.android.music.metachanged" -> {
                handleMetaChanged(context, intent)
            }
            "android.media.action.MEDIA_PLAY_FROM_SEARCH" -> {
                handleMediaPlayFromSearch(context, intent)
            }
        }
    }

    private fun handleSearchAction(context: Context, intent: Intent) {
        val query = intent.getStringExtra("query")?.lowercase()
        Log.d(TAG, "Search action: $query")

        receiverScope.launch {
            when {
                query?.contains("что за песня") == true ||
                        query?.contains("what song") == true ||
                        query?.contains("что играет") == true ||
                        query?.contains("what's playing") == true -> {
                    shareCurrentSongInfo(context)
                }
                query?.contains("включи радио") == true ||
                        query?.contains("play radio") == true -> {
                    startRadio(context)
                }
                query?.contains("выключи радио") == true ||
                        query?.contains("stop radio") == true -> {
                    stopRadio(context)
                }
                query?.contains("следующая станция") == true ||
                        query?.contains("next station") == true -> {
                    nextStation(context)
                }
                query?.contains("предыдущая станция") == true ||
                        query?.contains("previous station") == true -> {
                    previousStation(context)
                }
                else -> {
                    // Попробуем найти станцию по имени
                    findAndPlayStation(context, query ?: "")
                }
            }
        }
    }

    private fun handleVoiceCommand(context: Context, intent: Intent) {
        val results = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spokenText = results?.get(0)?.lowercase()

        Log.d(TAG, "Voice command: $spokenText")

        spokenText?.let { text ->
            receiverScope.launch {
                when {
                    text.contains("что за песня") ||
                            text.contains("что играет") ||
                            text.contains("скажи что за песня") -> {
                        shareCurrentSongInfo(context)
                    }
                    text.contains("включи радио") ||
                            text.contains("запусти радио") -> {
                        startRadio(context)
                    }
                    text.contains("выключи радио") ||
                            text.contains("останови радио") -> {
                        stopRadio(context)
                    }
                    text.contains("следующая") && text.contains("станция") -> {
                        nextStation(context)
                    }
                    text.contains("предыдущая") && text.contains("станция") -> {
                        previousStation(context)
                    }
                    text.contains("пауза") -> {
                        pauseRadio(context)
                    }
                    text.contains("играй") || text.contains("воспроизводи") -> {
                        findAndPlayStation(context, text)
                    }
                }
            }
        }
    }

    private fun handleMediaPlayFromSearch(context: Context, intent: Intent) {
        val query = intent.getStringExtra("query")
        Log.d(TAG, "Media play from search: $query")

        if (!query.isNullOrEmpty()) {
            receiverScope.launch {
                findAndPlayStation(context, query)
            }
        }
    }

    private fun handleMetaChanged(context: Context, intent: Intent) {
        // Обновляем метаданные для Google Assistant
        val artist = intent.getStringExtra("artist")
        val track = intent.getStringExtra("track")
        val album = intent.getStringExtra("album")
        val playing = intent.getBooleanExtra("playing", false)

        Log.d(TAG, "Meta changed: $artist - $track ($album) playing: $playing")
    }

    private suspend fun shareCurrentSongInfo(context: Context) {
        try {
            val currentStation = mediaManager.currentStation.value
            val currentMetadata = mediaManager.currentMetadata.value

            val songInfo = when {
                currentMetadata != null &&
                        currentMetadata.title.isNotBlank() &&
                        currentMetadata.artist.isNotBlank() -> {
                    "Сейчас играет: ${currentMetadata.artist} - ${currentMetadata.title} на станции ${currentStation?.name ?: "Unknown"}"
                }
                currentMetadata?.title?.isNotBlank() == true -> {
                    "Сейчас играет: ${currentMetadata.title} на станции ${currentStation?.name ?: "Unknown"}"
                }
                currentStation != null -> {
                    "Сейчас играет станция: ${currentStation.name}"
                }
                else -> {
                    "Радио не воспроизводится"
                }
            }

            // Отправляем результат Google Assistant
            sendAssistantResponse(context, songInfo)

            // Показываем уведомление с информацией
            showSongInfoNotification(context, songInfo)

            Log.d(TAG, "Shared song info: $songInfo")
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing song info", e)
            sendAssistantResponse(context, "Не удалось получить информацию о текущей песне")
        }
    }

    private suspend fun startRadio(context: Context) {
        try {
            val isPlaying = mediaManager.playbackState.value.isPlaying
            if (!isPlaying) {
                mediaManager.play()
                announceAction(context, "Радио включено")
            } else {
                announceAction(context, "Радио уже играет")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting radio", e)
            announceAction(context, "Не удалось включить радио")
        }
    }

    private suspend fun stopRadio(context: Context) {
        try {
            mediaManager.stop()
            announceAction(context, "Радио выключено")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping radio", e)
            announceAction(context, "Не удалось выключить радио")
        }
    }

    private suspend fun pauseRadio(context: Context) {
        try {
            mediaManager.pause()
            announceAction(context, "Радио поставлено на паузу")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing radio", e)
            announceAction(context, "Не удалось поставить на паузу")
        }
    }

    private suspend fun nextStation(context: Context) {
        try {
            mediaManager.skipToNext()
            val stationName = mediaManager.currentStation.value?.name ?: "Unknown"
            announceAction(context, "Переключено на станцию: $stationName")
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to next station", e)
            announceAction(context, "Не удалось переключить на следующую станцию")
        }
    }

    private suspend fun previousStation(context: Context) {
        try {
            mediaManager.skipToPrevious()
            val stationName = mediaManager.currentStation.value?.name ?: "Unknown"
            announceAction(context, "Переключено на станцию: $stationName")
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to previous station", e)
            announceAction(context, "Не удалось переключить на предыдущую станцию")
        }
    }

    private suspend fun findAndPlayStation(context: Context, query: String) {
        try {
            val stations = mediaManager.stationRepository.getAllStations().value
            val foundStation = stations.find { station ->
                station.name.contains(query, ignoreCase = true) ||
                        station.categories.any { it.contains(query, ignoreCase = true) }
            }

            if (foundStation != null) {
                mediaManager.play(foundStation)
                announceAction(context, "Включена станция: ${foundStation.name}")
            } else {
                announceAction(context, "Станция не найдена: $query")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding and playing station", e)
            announceAction(context, "Не удалось найти станцию: $query")
        }
    }

    private fun sendAssistantResponse(context: Context, message: String) {
        try {
            // Отправка ответа Google Assistant
            val resultIntent = Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH_RESULT").apply {
                putExtra("query", message)
                putExtra("result", message)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(resultIntent)

            // Альтернативный способ для новых версий
            val textResponseIntent = Intent("com.google.android.googlequicksearchbox.TEXT_RESPONSE").apply {
                putExtra("text", message)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(textResponseIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending assistant response", e)
        }
    }

    private fun announceAction(context: Context, message: String) {
        sendAssistantResponse(context, message)

        // Также можем использовать TTS
        try {
            val ttsIntent = Intent("android.speech.tts.engine.SYNTHESIZE_DATA").apply {
                putExtra("text", message)
                putExtra("pitch", 1.0f)
                putExtra("rate", 1.0f)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(ttsIntent)
        } catch (e: Exception) {
            Log.w(TAG, "TTS not available", e)
        }
    }

    private fun showSongInfoNotification(context: Context, songInfo: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager

            // Создаем канал для Android O+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "voice_response_channel",
                    "Voice Response",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Ответы на голосовые команды"
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = androidx.core.app.NotificationCompat.Builder(context, "voice_response_channel")
                .setContentTitle("Voice Command Response")
                .setContentText(songInfo)
                .setSmallIcon(R.drawable.logo)
                .setAutoCancel(true)
                .setTimeoutAfter(5000) // Автоматически скрыть через 5 секунд
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
}