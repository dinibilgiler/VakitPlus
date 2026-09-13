package com.vakitplus.app

import android.content.Context

data class PrayerNotificationPreference(
    val enabled: Boolean = true,
    val beforeMinutes: Int = 0
)

object NotificationSettings {
    private const val PREF = "vakit_plus_notifications"
    private val names = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
    private val allowedOffsets = listOf(0, 5, 10, 15, 30)

    fun names() = names
    fun offsets() = allowedOffsets

    fun enabled(context: Context, name: String): Boolean =
        context.getSharedPreferences(PREF, 0).getBoolean("enabled_$name", true)

    fun beforeMinutes(context: Context, name: String): Int =
        context.getSharedPreferences(PREF, 0).getInt("before_$name", 0)
            .coerceIn(allowedOffsets.minOrNull() ?: 0, allowedOffsets.maxOrNull() ?: 30)

    fun save(context: Context, name: String, enabled: Boolean, beforeMinutes: Int) {
        context.getSharedPreferences(PREF, 0).edit()
            .putBoolean("enabled_$name", enabled)
            .putInt("before_$name", beforeMinutes.coerceIn(0, 30))
            .apply()
    }
}
