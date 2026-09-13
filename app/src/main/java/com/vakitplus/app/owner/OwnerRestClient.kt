package com.vakitplus.app.owner

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Lightweight REST client. Credentials are sent only to the configured HTTPS backend. */
class OwnerRestClient(private val baseUrl: String) : OwnerApi {
    private fun request(method: String, path: String, token: String? = null, body: String? = null): JSONObject {
        val clean = baseUrl.trimEnd('/')
        val conn = (URL(clean + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        body?.let { conn.outputStream.use { out -> out.write(it.toByteArray(Charsets.UTF_8)) } }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("Sunucu hatası ($code): $text")
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    override suspend fun signIn(identifier: String, credential: String, integrityToken: String?): OwnerSession {
        val body = JSONObject().apply {
            put("identifier", identifier)
            put("password", credential)
            if (!integrityToken.isNullOrBlank()) put("integrityToken", integrityToken)
        }
        return parseSession(request("POST", "/v1/auth/login", body = body.toString()))
    }

    override suspend fun refresh(refreshToken: String): OwnerSession =
        parseSession(request("POST", "/v1/auth/refresh", body = JSONObject().put("refreshToken", refreshToken).toString()))

    override suspend fun signOut(refreshToken: String) {
        request("POST", "/v1/auth/logout", body = JSONObject().put("refreshToken", refreshToken).toString())
    }

    override suspend fun getContent(accessToken: String): OwnerContent {
        val o = request("GET", "/v1/content", accessToken)
        return OwnerContent(o.optString("appSubtitle"), o.optString("dailyMessage"), o.optString("adminNote"), o.optLong("version"))
    }

    override suspend fun updateContent(accessToken: String, content: OwnerContent): OwnerContent {
        val body = JSONObject().apply {
            put("appSubtitle", content.appSubtitle)
            put("dailyMessage", content.dailyMessage)
            put("adminNote", content.adminNote)
            put("version", content.version)
        }
        val o = request("PUT", "/v1/content", accessToken, body.toString())
        return OwnerContent(o.optString("appSubtitle"), o.optString("dailyMessage"), o.optString("adminNote"), o.optLong("version"))
    }

    override suspend fun sendNotification(accessToken: String, campaign: NotificationCampaign): NotificationSendResult {
        val body = JSONObject().apply { put("title", campaign.title); put("body", campaign.body); put("topic", campaign.topic); campaign.scheduledAtEpochMillis?.let { put("scheduledAtEpochMillis", it) } }
        val o = request("POST", "/v1/notifications/send", accessToken, body.toString())
        return NotificationSendResult(o.optString("messageId"), o.optString("target"))
    }

    override suspend fun getDevices(accessToken: String): DeviceRegistry {
        val o=request("GET","/v1/devices",accessToken); val a=o.optJSONArray("devices"); val list=mutableListOf<DeviceInfo>();
        for(i in 0 until (a?.length() ?: 0)){ val d=a!!.getJSONObject(i); list += DeviceInfo(d.getString("id"),d.optString("platform"),d.optString("appVersion"),d.optString("city"),d.optBoolean("notificationsEnabled",true),d.optLong("createdAt"),d.optLong("lastSeen")) }
        val sm=o.optJSONObject("summary"); return DeviceRegistry(list,DeviceSummary(sm?.optInt("total",list.size)?:list.size,sm?.optInt("active",0)?:0,sm?.optInt("notificationsEnabled",0)?:0))
    }

    override suspend fun removeDevice(accessToken: String, deviceId: String) {
        request("DELETE","/v1/devices/" + java.net.URLEncoder.encode(deviceId,"UTF-8"),accessToken)
    }

    override suspend fun dashboardSummary(accessToken: String): DashboardSummary {
        val o=request("GET","/v1/dashboard/summary",accessToken)
        return DashboardSummary(o.optInt("users"),o.optInt("devices"),o.optInt("activeDevices"),o.optInt("notificationsEnabled"),o.optInt("notificationsSent"),o.optInt("scheduledNotifications"),o.optInt("drafts"),o.optInt("auditEvents"),o.optBoolean("maintenance"))
    }
    override suspend fun getSystemSettings(accessToken: String): AppSystemSettings {
        val o=request("GET","/v1/system/settings",accessToken)
        return AppSystemSettings(o.optBoolean("maintenance"),o.optString("announcement"),o.optLong("updatedAt"))
    }
    override suspend fun updateSystemSettings(accessToken: String, settings: AppSystemSettings): AppSystemSettings {
        val o=request("PUT","/v1/system/settings",accessToken,JSONObject().put("maintenance",settings.maintenance).put("announcement",settings.announcement).toString())
        return AppSystemSettings(o.optBoolean("maintenance"),o.optString("announcement"),o.optLong("updatedAt"))
    }
    override suspend fun auditLog(accessToken: String, event: AuditEvent) {
        val body = JSONObject().apply { put("action", event.action); put("actor", event.actor); put("timestamp", event.timestampEpochMillis); put("details", event.details) }
        request("POST", "/v1/audit", accessToken, body.toString())
    }

    override suspend fun getNotificationHistory(accessToken: String): Pair<List<NotificationRecord>, List<ScheduledNotification>> {
        val o=request("GET","/v1/notifications/history",accessToken); val sent=mutableListOf<NotificationRecord>(); val a=o.optJSONArray("notifications");
        for(i in 0 until (a?.length() ?: 0)){ val d=a!!.getJSONObject(i); sent += NotificationRecord(d.optString("id"),d.optString("title"),d.optString("body"),d.optString("topic"),d.optLong("timestamp"),d.optString("status"),d.optString("messageId")) }
        val sch=mutableListOf<ScheduledNotification>(); val q=o.optJSONArray("scheduled");
        for(i in 0 until (q?.length() ?: 0)){ val d=q!!.getJSONObject(i); sch += ScheduledNotification(d.optString("id"),d.optString("title"),d.optString("body"),d.optString("topic"),d.optLong("scheduledAtEpochMillis"),d.optString("status")) }
        return sent to sch
    }
    override suspend fun scheduleNotification(accessToken:String,campaign:NotificationCampaign): ScheduledNotification { val b=JSONObject().apply{put("title",campaign.title);put("body",campaign.body);put("topic",campaign.topic);put("scheduledAtEpochMillis",campaign.scheduledAtEpochMillis ?: error("Zaman gerekli"))}; val d=request("POST","/v1/notifications/schedule",accessToken,b.toString()); return ScheduledNotification(d.optString("id"),d.optString("title"),d.optString("body"),d.optString("topic"),d.optLong("scheduledAtEpochMillis"),d.optString("status")) }
    override suspend fun cancelScheduledNotification(accessToken:String,id:String){ request("DELETE","/v1/notifications/schedule/"+java.net.URLEncoder.encode(id,"UTF-8"),accessToken) }
    override suspend fun getNotificationStats(accessToken:String): NotificationStats { val o=request("GET","/v1/notifications/stats",accessToken); val m=mutableMapOf<String,Int>(); val bo=o.optJSONObject("byTopic"); bo?.keys()?.forEach{m[it]=bo.optInt(it)}; return NotificationStats(o.optInt("totalSent"),o.optInt("totalScheduled"),o.optInt("totalDrafts"),m) }

    private fun parseSession(o: JSONObject): OwnerSession = OwnerSession(
        o.getString("userId"), o.optString("displayName"), OwnerRole.valueOf(o.getString("role")),
        o.getString("accessToken"), o.getString("refreshToken"), o.getLong("expiresAtEpochSeconds")
    )
}
