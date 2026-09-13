package com.vakitplus.app.owner

import android.content.Context

object RemoteOwnerConfig {
    private const val PREFS = "vakit_plus_remote_owner"
    private const val URL = "backend_url"
    fun getBaseUrl(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(URL, "") ?: ""
    fun setBaseUrl(context: Context, value: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(URL, value.trim().trimEnd('/')).apply()
}
