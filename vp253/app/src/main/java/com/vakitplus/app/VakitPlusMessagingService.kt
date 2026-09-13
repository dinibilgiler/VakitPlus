package com.vakitplus.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class VakitPlusMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Vakit+"
        val body = message.notification?.body ?: message.data["body"] ?: "Yeni bildirim"
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val channelId = "owner_broadcasts"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(channelId, "Vakit+ Duyuruları", NotificationManager.IMPORTANCE_DEFAULT))
        val n = NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_vakitplus).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token registration endpoint can be connected in the next user/device management stage.
    }
}
