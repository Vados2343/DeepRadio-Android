package com.myradio.deepradio

import android.content.Context

object AppSettingsManager {
    private const val PREFS_NAME = "DeepRadioPrefs"
    private const val KEY_AUTO_ROTATE = "auto_rotate_enabled"

    fun isAutoRotateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_ROTATE, true)
    }

    fun setAutoRotateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_ROTATE, enabled).apply()
    }
}
