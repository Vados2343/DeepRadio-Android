package com.myradio.deepradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.Keep
import com.myradio.deepradio.domain.MediaManager
import javax.inject.Inject
@Keep
class StopServiceReceiver : BroadcastReceiver() {
    @Inject
        lateinit var mediaManager: MediaManager

        override fun onReceive(context: Context, intent: Intent) {
            try {
                mediaManager.stop()
                (context as? android.app.Activity)?.let { activity ->
                    activity.finishAndRemoveTask()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            } catch (e: Exception) {
                Log.e("StopServiceReceiver", "Error stopping service", e)
            }
        }
    }
data class AssistantResponse(
    val text: String,
    val type: ResponseType = ResponseType.TEXT
)

enum class ResponseType {
    TEXT,
    SONG_INFO,
    ERROR
}
