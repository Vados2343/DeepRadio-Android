// src/main/java/com/myradio/deepradio/DeepRadioApplication.kt
package com.myradio.deepradio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DeepRadioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // любая инициализация
    }
}
