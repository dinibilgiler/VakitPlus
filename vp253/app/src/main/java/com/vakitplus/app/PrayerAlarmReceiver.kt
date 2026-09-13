package com.vakitplus.app

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.createChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val name = intent.getStringExtra("name") ?: "Namaz"
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        val text = if (minutesBefore > 0) "$name vaktine $minutesBefore dakika kaldı." else "$name vakti geldi."
        val notification = NotificationCompat.Builder(context, "prayer_times")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Vakit+ • $name")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(name.hashCode() + minutesBefore, notification)
    }
}

class ScheduleRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> NotificationScheduler.scheduleTomorrowFromSavedCity(context)
        }
    }
}
