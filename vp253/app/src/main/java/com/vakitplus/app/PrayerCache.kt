package com.vakitplus.app

import android.content.Context
import java.time.LocalDate

object PrayerCache {
    private const val PREF = "prayer_cache"
    fun save(context: Context, city: City, date: LocalDate, t: PrayerTimes) {
        context.getSharedPreferences(PREF,0).edit()
            .putString("${city.name}_${date}_imsak",t.imsak)
            .putString("${city.name}_${date}_sunrise",t.sunrise)
            .putString("${city.name}_${date}_dhuhr",t.dhuhr)
            .putString("${city.name}_${date}_asr",t.asr)
            .putString("${city.name}_${date}_maghrib",t.maghrib)
            .putString("${city.name}_${date}_isha",t.isha).apply()
    }
    fun read(context: Context, city: City, date: LocalDate): PrayerTimes? {
        val p=context.getSharedPreferences(PREF,0)
        fun g(k:String)=p.getString("${city.name}_${date}_$k",null) ?: return null
        return PrayerTimes(g("imsak"),g("sunrise"),g("dhuhr"),g("asr"),g("maghrib"),g("isha"))
    }
}
