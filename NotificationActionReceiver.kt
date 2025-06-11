package com.myradio.deepradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val serviceIntent = Intent(context, RadioPlaybackService::class.java).apply {
            this.action = action
        }
        context.startService(serviceIntent)
    }
}
