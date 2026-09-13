package com.vakitplus.app.owner

/** Server-authoritative roles. OWNER is the only role allowed to change ownership/security settings. */
enum class OwnerRole { OWNER, ADMIN, EDITOR }

data class OwnerSession(
    val userId: String,
    val displayName: String,
    val role: OwnerRole,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
)

data class OwnerContent(
    val appSubtitle: String,
    val dailyMessage: String,
    val adminNote: String,
    val version: Long
)

data class AuditEvent(
    val action: String,
    val actor: String,
    val timestampEpochMillis: Long,
    val details: String = ""
)


data class NotificationCampaign(val title: String, val body: String, val topic: String = "all", val scheduledAtEpochMillis: Long? = null)
data class NotificationSendResult(val messageId: String, val target: String)


data class DeviceInfo(
    val id: String, val platform: String, val appVersion: String, val city: String,
    val notificationsEnabled: Boolean, val createdAt: Long, val lastSeen: Long
)
data class DeviceSummary(val total: Int, val active: Int, val notificationsEnabled: Int)
data class DeviceRegistry(val devices: List<DeviceInfo>, val summary: DeviceSummary)


data class NotificationRecord(val id:String,val title:String,val body:String,val topic:String,val timestamp:Long,val status:String,val messageId:String="")
data class ScheduledNotification(val id:String,val title:String,val body:String,val topic:String,val scheduledAt:Long,val status:String)
data class NotificationStats(val totalSent:Int,val totalScheduled:Int,val totalDrafts:Int,val byTopic:Map<String,Int>)
