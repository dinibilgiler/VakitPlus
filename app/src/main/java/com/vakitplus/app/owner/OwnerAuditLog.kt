package com.vakitplus.app.owner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class OwnerAuditLog(context: Context) {
    private val prefs = context.getSharedPreferences("vakit_plus_owner_audit", Context.MODE_PRIVATE)

    fun append(event: AuditEvent) {
        val old = runCatching { JSONArray(prefs.getString("events", "[]")) }.getOrElse { JSONArray() }
        old.put(JSONObject().apply {
            put("action", event.action); put("actor", event.actor)
            put("timestamp", event.timestampEpochMillis); put("details", event.details)
        })
        val start = (old.length() - 100).coerceAtLeast(0)
        val trimmed = JSONArray()
        for (i in start until old.length()) trimmed.put(old.get(i))
        prefs.edit().putString("events", trimmed.toString()).apply()
    }

    fun recent(limit: Int = 20): List<AuditEvent> {
        val arr = runCatching { JSONArray(prefs.getString("events", "[]")) }.getOrElse { JSONArray() }
        val start = (arr.length() - limit).coerceAtLeast(0)
        return (start until arr.length()).mapNotNull { i -> runCatching {
            val o = arr.getJSONObject(i)
            AuditEvent(o.optString("action"), o.optString("actor"), o.optLong("timestamp"), o.optString("details"))
        }.getOrNull() }.reversed()
    }
}
