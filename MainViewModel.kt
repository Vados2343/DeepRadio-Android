package com.myradio.deepradio.domain.com.example.deepradio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myradio.deepradio.UpdateManager
import com.myradio.deepradio.domain.MediaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaManager: MediaManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState = combine(
        _message,
        mediaManager.playbackState
    ) { message, playbackState ->
        MainUiState(
            message = message,
            isPlaying = playbackState.isPlaying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                updateManager.checkForUpdates()
            } catch (e: Exception) {
                _message.value = "Не удалось проверить обновления"
            }
        }
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }
}

data class MainUiState(
    val message: String? = null,
    val isPlaying: Boolean = false
)