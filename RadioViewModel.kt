package com.myradio.deepradio.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myradio.deepradio.RadioStation
import com.myradio.deepradio.domain.MediaManager
import com.myradio.deepradio.domain.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    application: Application,
    private val mediaManager: MediaManager,
    private val stationRepository: StationRepository,
    private val googleAssistantHandler: GoogleAssistantHandler
) : AndroidViewModel(application) {

    // UI States
    val playbackState = mediaManager.playbackState
    val currentStation = mediaManager.currentStation
    val currentMetadata = mediaManager.currentMetadata
    val isBuffering = mediaManager.isBuffering
    val expandedPlayer = mediaManager.expandedPlayer
    val viewMode = mediaManager.viewMode

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _showVoiceResult = MutableStateFlow<VoiceResult?>(null)
    val showVoiceResult: StateFlow<VoiceResult?> = _showVoiceResult.asStateFlow()

    val categories = stationRepository.categories
    val favorites = stationRepository.favorites

    val filteredStations = combine(
        _selectedCategory,
        _searchQuery
    ) { category, search ->
        Pair(category, search)
    }.flatMapLatest { (category, search) ->
        stationRepository.getFilteredStations(category, search)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Регистрируем обработчик Google Assistant
        googleAssistantHandler.registerVoiceCommands { command ->
            handleVoiceCommand(command)
        }
    }

    fun onStationClick(station: RadioStation) {
        mediaManager.play(station)
    }

    fun onPlayPauseClick() {
        if (playbackState.value.isPlaying) {
            mediaManager.pause()
        } else {
            mediaManager.play()
        }
    }

    fun onNextClick() {
        mediaManager.skipToNext()
    }

    fun onPreviousClick() {
        mediaManager.skipToPrevious()
    }

    fun onFavoriteClick(station: RadioStation) {
        stationRepository.toggleFavorite(station)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = category
    }

    fun onViewModeToggle() {
        mediaManager.toggleViewMode()
    }

    fun onExpandedPlayerChange(expanded: Boolean) {
        mediaManager.setExpandedPlayer(expanded)
    }

    fun onShareClick() {
        val songInfo = mediaManager.getCurrentSongInfo()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, songInfo)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share song info")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooserIntent)
    }

    private fun handleVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            when (command) {
                is VoiceCommand.WhatSong -> {
                    val result = VoiceResult(
                        title = "Current Song",
                        message = mediaManager.getCurrentSongInfo(),
                        action = VoiceAction.ShowInfo
                    )
                    _showVoiceResult.value = result
                }
                is VoiceCommand.PlayStation -> {
                    val station = filteredStations.value.find {
                        it.name.contains(command.stationName, ignoreCase = true)
                    }
                    if (station != null) {
                        mediaManager.play(station)
                        _showVoiceResult.value = VoiceResult(
                            title = "Playing",
                            message = "Now playing ${station.name}",
                            action = VoiceAction.PlayStation
                        )
                    }
                }
                is VoiceCommand.NextStation -> {
                    mediaManager.skipToNext()
                }
                is VoiceCommand.PreviousStation -> {
                    mediaManager.skipToPrevious()
                }
                is VoiceCommand.Pause -> {
                    mediaManager.pause()
                }
                is VoiceCommand.Play -> {
                    mediaManager.play()
                }
                else -> {
                    // Ничего не делать или логировать неизвестную команду
                }
            }
        }
    }


    fun dismissVoiceResult() {
        _showVoiceResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        googleAssistantHandler.unregister()
    }
}

// Google Assistant Handler
class GoogleAssistantHandler @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    private var voiceCommandCallback: ((VoiceCommand) -> Unit)? = null
    private val voiceReceiver = VoiceCommandReceiver()

    fun registerVoiceCommands(callback: (VoiceCommand) -> Unit) {
        voiceCommandCallback = callback

        val filter = android.content.IntentFilter().apply {
            addAction("android.intent.action.VOICE_COMMAND")
            addAction("com.google.android.gms.actions.SEARCH_ACTION")
            addAction("android.media.action.MEDIA_PLAY_FROM_SEARCH")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(voiceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(voiceReceiver, filter)
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(voiceReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    inner class VoiceCommandReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            intent?.let { processVoiceIntent(it) }
        }
    }

    private fun processVoiceIntent(intent: Intent) {
        val query = intent.getStringExtra(RecognizerIntent.EXTRA_RESULTS)
            ?: intent.getStringExtra("query")
            ?: return

        val command = parseVoiceCommand(query)
        voiceCommandCallback?.invoke(command)
    }

    private fun parseVoiceCommand(query: String): VoiceCommand {
        val lowerQuery = query.lowercase()

        return when {
            lowerQuery.contains("what") && lowerQuery.contains("song") ||
                    lowerQuery.contains("что") && lowerQuery.contains("песня") ||
                    lowerQuery.contains("какая") && lowerQuery.contains("песня") -> {
                VoiceCommand.WhatSong
            }
            lowerQuery.contains("play") && lowerQuery.contains("station") ||
                    lowerQuery.contains("включи") && lowerQuery.contains("станци") -> {
                val stationName = extractStationName(query)
                VoiceCommand.PlayStation(stationName)
            }
            lowerQuery.contains("next") || lowerQuery.contains("следующ") -> {
                VoiceCommand.NextStation
            }
            lowerQuery.contains("previous") || lowerQuery.contains("предыдущ") -> {
                VoiceCommand.PreviousStation
            }
            lowerQuery.contains("pause") || lowerQuery.contains("пауза") -> {
                VoiceCommand.Pause
            }
            lowerQuery.contains("play") || lowerQuery.contains("играть") -> {
                VoiceCommand.Play
            }
            else -> VoiceCommand.WhatSong
        }
    }

    private fun extractStationName(query: String): String {
        // Extract station name from query
        val words = query.split(" ")
        val stationIndex = words.indexOfFirst {
            it.equals("station", ignoreCase = true) ||
                    it.equals("станцию", ignoreCase = true)
        }
        return if (stationIndex != -1 && stationIndex < words.size - 1) {
            words.subList(stationIndex + 1, words.size).joinToString(" ")
        } else {
            ""
        }
    }
}

// Voice Command Models
sealed class VoiceCommand {
    object WhatSong : VoiceCommand()
    data class PlayStation(val stationName: String) : VoiceCommand()
    object NextStation : VoiceCommand()
    object PreviousStation : VoiceCommand()
    object Pause : VoiceCommand()
    object Play : VoiceCommand()
}

data class VoiceResult(
    val title: String,
    val message: String,
    val action: VoiceAction
)

enum class VoiceAction {
    ShowInfo, PlayStation, Error
}