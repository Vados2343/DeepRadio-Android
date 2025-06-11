package com.myradio.deepradio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _suggestionsState = MutableStateFlow<SuggestionsState>(SuggestionsState.Idle)
    val suggestionsState: StateFlow<SuggestionsState> = _suggestionsState

    val snackbarMessages = MutableSharedFlow<String>()

    fun sendSuggestion(
        stationName: String?,
        stationStream: String?,
        stationIcon: String?,
        songStream: String?,
        suggestion: String?
    ) {
        _suggestionsState.value = SuggestionsState.Loading
        viewModelScope.launch {
            try {
                val emailContent = buildString {
                    stationName?.let { append("Станция: $it\n") }
                    stationStream?.let { append("Поток: $it\n") }
                    stationIcon?.let { append("Иконка: $it\n") }
                    songStream?.let { append("Поток с песней: $it\n") }
                    suggestion?.let { append("Предложение: $it\n") }
                }

                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@deepradio.site"))
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Предложение")
                    putExtra(android.content.Intent.EXTRA_TEXT, emailContent)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(android.content.Intent.createChooser(emailIntent, "Отправить email..."))
                _suggestionsState.value = SuggestionsState.Success
            } catch (e: Exception) {
                _suggestionsState.value = SuggestionsState.Error("Ошибка отправки: ${e.message}")
            }
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            snackbarMessages.emit(message)
        }
    }
}

sealed class SuggestionsState {
    object Idle : SuggestionsState()
    object Loading : SuggestionsState()
    object Success : SuggestionsState()
    data class Error(val message: String) : SuggestionsState()
}