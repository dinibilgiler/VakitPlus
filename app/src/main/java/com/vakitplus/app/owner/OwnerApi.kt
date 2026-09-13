package com.vakitplus.app.owner

/**
 * Backend contract for the production owner system.
 * The Android client must never decide ownership by itself; the server does.
 */
interface OwnerApi {
    suspend fun signIn(identifier: String, credential: String, integrityToken: String?): OwnerSession
    suspend fun refresh(refreshToken: String): OwnerSession
    suspend fun signOut(refreshToken: String)
    suspend fun getContent(accessToken: String): OwnerContent
    suspend fun updateContent(accessToken: String, content: OwnerContent): OwnerContent
    suspend fun auditLog(accessToken: String, event: AuditEvent)
    suspend fun sendNotification(accessToken: String, campaign: NotificationCampaign): NotificationSendResult
    suspend fun getDevices(accessToken: String): DeviceRegistry
    suspend fun getNotificationHistory(accessToken: String): Pair<List<NotificationRecord>, List<ScheduledNotification>>
    suspend fun scheduleNotification(accessToken: String, campaign: NotificationCampaign): ScheduledNotification
    suspend fun cancelScheduledNotification(accessToken: String, id: String)
    suspend fun getNotificationStats(accessToken: String): NotificationStats
    suspend fun removeDevice(accessToken: String, deviceId: String)
    suspend fun dashboardSummary(accessToken: String): DashboardSummary
    suspend fun getSystemSettings(accessToken: String): AppSystemSettings
    suspend fun updateSystemSettings(accessToken: String, settings: AppSystemSettings): AppSystemSettings
}
