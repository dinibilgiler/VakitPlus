package com.vakitplus.app

import android.content.Context

object AdminConfig {
    private const val PREF = "vakit_plus_admin_config"
    private fun p(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun appSubtitle(context: Context) = p(context).getString("subtitle", "Daha anlamlı bir gün için") ?: "Daha anlamlı bir gün için"
    fun dailyMessage(context: Context) = p(context).getString("daily_message", "Hayırlı ve huzurlu bir gün dileriz.") ?: "Hayırlı ve huzurlu bir gün dileriz."
    fun adminNote(context: Context) = p(context).getString("admin_note", "") ?: ""

    fun save(context: Context, subtitle: String, dailyMessage: String, adminNote: String) {
        p(context).edit()
            .putString("subtitle", subtitle)
            .putString("daily_message", dailyMessage)
            .putString("admin_note", adminNote)
            .apply()
    }
}
