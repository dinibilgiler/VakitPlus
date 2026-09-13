package com.vakitplus.app

import android.app.*
import android.content.*
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

object NotificationScheduler {
    private const val CHANNEL_ID = "prayer_times"
    private const val CHANNEL_NAME = "Namaz Vakitleri"
    private const val REQUEST_BASE = 5000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Namaz vakitleri ve hatırlatmalar"
                }
            )
        }
    }

    fun scheduleNextDay(context: Context, times: PrayerTimes) {
        createChannel(context)
        cancelAll(context)
        scheduleForDate(context, LocalDate.now().plusDays(1), times)
    }

    fun scheduleForDate(context: Context, date: LocalDate, times: PrayerTimes) {
        createChannel(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        times.asList().forEachIndexed { index, (name, value) ->
            if (!NotificationSettings.enabled(context, name)) return@forEachIndexed
            val minutesBefore = NotificationSettings.beforeMinutes(context, name)
            val time = runCatching { LocalTime.parse(value) }.getOrNull() ?: return@forEachIndexed
            val trigger = Calendar.getInstance().apply {
                set(Calendar.YEAR, date.year)
                set(Calendar.DAY_OF_YEAR, date.dayOfYear)
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -minutesBefore)
            }.timeInMillis
            if (trigger <= System.currentTimeMillis()) return@forEachIndexed

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra("name", name)
                putExtra("minutesBefore", minutesBefore)
            }
            val requestCode = REQUEST_BASE + date.dayOfYear * 10 + index
            val pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        }
    }

    fun scheduleTomorrowFromSavedCity(context: Context) {
        val city = AppSettings.city(context)
        val date = LocalDate.now().plusDays(1)
        val times = PrayerEngine.calculate(date, city.lat, city.lon, city.tz)
        scheduleForDate(context, date, times)
    }

    fun cancelAll(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        NotificationSettings.names().forEachIndexed { index, name ->
            for (dayOffset in 0..2) {
                val date = LocalDate.now().plusDays(dayOffset.toLong())
                val requestCode = REQUEST_BASE + date.dayOfYear * 10 + index
                val intent = Intent(context, PrayerAlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) alarms.cancel(pi)
            }
        }
    }
}
