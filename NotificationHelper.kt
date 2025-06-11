package com.myradio.deepradio

import android.content.Context

object NotificationHelper {

    fun showNetworkNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "network_channel",
                "Network Status",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о состоянии сети"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, "network_channel")
            .setContentTitle("Deep Radio - Сеть")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setTimeoutAfter(8000) // Автоматически скрыть через 8 секунд
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}