package com.vakitplus.app.owner

import android.content.Context

class RemoteOwnerRepository(private val context: Context) {
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
}


suspend fun RemoteOwnerRepository.loadDevices(): DeviceRegistry {
    val s=session() ?: error("Merkezi Owner oturumu bulunamadı.")
    return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).getDevices(s.accessToken)
}

suspend fun RemoteOwnerRepository.removeDevice(deviceId: String) {
    val s=session() ?: error("Merkezi Owner oturumu bulunamadı.")
    OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).removeDevice(s.accessToken, deviceId)
}


suspend fun RemoteOwnerRepository.notificationHistory(): Pair<List<NotificationRecord>, List<ScheduledNotification>> { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).getNotificationHistory(s.accessToken) }
suspend fun RemoteOwnerRepository.scheduleNotification(campaign: NotificationCampaign): ScheduledNotification { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).scheduleNotification(s.accessToken,campaign) }
suspend fun RemoteOwnerRepository.cancelScheduledNotification(id:String) { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).cancelScheduledNotification(s.accessToken,id) }
suspend fun RemoteOwnerRepository.notificationStats(): NotificationStats { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).getNotificationStats(s.accessToken) }


suspend fun RemoteOwnerRepository.dashboardSummary(): DashboardSummary { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).dashboardSummary(s.accessToken) }
suspend fun RemoteOwnerRepository.getSystemSettings(): AppSystemSettings { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).getSystemSettings(s.accessToken) }
suspend fun RemoteOwnerRepository.updateSystemSettings(maintenance:Boolean, announcement:String): AppSystemSettings { val s=session() ?: error("Merkezi Owner oturumu bulunamadı."); return OwnerRestClient(RemoteOwnerConfig.getBaseUrl(context)).updateSystemSettings(s.accessToken, AppSystemSettings(maintenance,announcement,System.currentTimeMillis())) }
