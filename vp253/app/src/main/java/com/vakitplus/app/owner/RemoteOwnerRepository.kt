package com.vakitplus.app.owner

import android.content.Context

class RemoteOwnerRepository(val context: Context) {
    private val store = SecureSessionStore(context)

    private fun api(): OwnerRestClient {
        val url = RemoteOwnerConfig.getBaseUrl(context)
        require(url.startsWith("https://")) { "Owner sunucusu için HTTPS adresi girin." }
        return OwnerRestClient(url)
    }

    suspend fun login(email: String, password: String): OwnerSession {
        val session = api().signIn(email.trim(), password, null)
        require(session.role == OwnerRole.OWNER) { "Bu hesap OWNER yetkisine sahip değil." }
        store.save(session)
        OwnerAuditLog(context).append(AuditEvent("REMOTE_OWNER_LOGIN", session.userId, System.currentTimeMillis(), "Merkezi Owner girişi"))
        return session
    }

    suspend fun loadContent(): OwnerContent {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().getContent(s.accessToken)
    }

    suspend fun updateContent(content: OwnerContent): OwnerContent {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        val updated = api().updateContent(s.accessToken, content)
        OwnerAuditLog(context).append(AuditEvent("REMOTE_CONTENT_UPDATE", s.userId, System.currentTimeMillis(), "Merkezi içerik güncellendi"))
        return updated
    }

    fun session(): OwnerSession? = store.load()
    fun logout() { store.clear() }

    suspend fun sendNotification(campaign: NotificationCampaign): NotificationSendResult {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().sendNotification(s.accessToken, campaign).also {
            OwnerAuditLog(context).append(AuditEvent("REMOTE_NOTIFICATION_SEND", s.userId, System.currentTimeMillis(), "Bildirim gönderildi: ${campaign.title}"))
        }
    }

    suspend fun loadDevices(): DeviceRegistry {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().getDevices(s.accessToken)
    }

    suspend fun removeDevice(deviceId: String) {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        api().removeDevice(s.accessToken, deviceId)
    }

    suspend fun notificationHistory(): Pair<List<NotificationRecord>, List<ScheduledNotification>> {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().getNotificationHistory(s.accessToken)
    }

    suspend fun scheduleNotification(campaign: NotificationCampaign): ScheduledNotification {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().scheduleNotification(s.accessToken, campaign)
    }

    suspend fun cancelScheduledNotification(id: String) {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        api().cancelScheduledNotification(s.accessToken, id)
    }

    suspend fun notificationStats(): NotificationStats {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().getNotificationStats(s.accessToken)
    }

    suspend fun dashboardSummary(): DashboardSummary {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().dashboardSummary(s.accessToken)
    }

    suspend fun getSystemSettings(): AppSystemSettings {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().getSystemSettings(s.accessToken)
    }

    suspend fun updateSystemSettings(maintenance: Boolean, announcement: String): AppSystemSettings {
        val s = store.load() ?: error("Merkezi Owner oturumu bulunamadı.")
        return api().updateSystemSettings(s.accessToken, AppSystemSettings(maintenance, announcement, System.currentTimeMillis()))
    }
}
