package com.vakitplus.app

import java.time.LocalDate
import kotlin.math.*

data class PrayerTimes(
    val imsak: String, val sunrise: String, val dhuhr: String,
    val asr: String, val maghrib: String, val isha: String
) {
    fun asList() = listOf(
        "İmsak" to imsak, "Güneş" to sunrise, "Öğle" to dhuhr,
        "İkindi" to asr, "Akşam" to maghrib, "Yatsı" to isha
    )
}

object PrayerEngine {
    fun calculate(date: LocalDate, lat: Double, lon: Double, tz: Double): PrayerTimes {
        val jd = julian(date.year, date.monthValue, date.dayOfMonth)
        val decl = solarDeclination(jd)
        val eq = equationOfTime(jd)
        val noon = 720.0 - 4.0 * lon - eq + tz * 60.0
        fun time(angle: Double, dir: Int): Double {
            val x = (cos(Math.toRadians(angle)) /
                    (cos(Math.toRadians(lat)) * cos(Math.toRadians(decl))) -
                    tan(Math.toRadians(lat)) * tan(Math.toRadians(decl)))
                .coerceIn(-1.0, 1.0)
            val ha = Math.toDegrees(acos(x))
            return noon + dir * 4.0 * ha
        }
        val sunrise = time(90.833, -1)
        val sunset = time(90.833, 1)
        val fajr = time(18.0, -1)
        val isha = time(17.0, 1)

        // Hanafi shadow factor 1.
        val altitude = Math.toDegrees(
            atan(1.0 / (1.0 + tan(abs(Math.toRadians(lat - decl)))))
        )
        val asr = time(90.0 - altitude, 1)

        return PrayerTimes(fmt(fajr), fmt(sunrise), fmt(noon), fmt(asr), fmt(sunset), fmt(isha))
    }

    private fun fmt(minutes: Double): String {
        var m = ((minutes + .5).toInt()) % 1440
        if (m < 0) m += 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }
    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }
    private fun solarDeclination(jd: Double): Double {
        val n = jd - 2451545.0
        val g = Math.toRadians((357.529 + .98560028 * n) % 360)
        val q = (280.459 + .98564736 * n) % 360
        val l = Math.toRadians((q + 1.915 * sin(g) + .020 * sin(2*g)) % 360)
        return Math.toDegrees(asin(sin(Math.toRadians(23.439 - .00000036*n)) * sin(l)))
    }
    private fun equationOfTime(jd: Double): Double {
        val n = jd - 2451545.0
        val g = Math.toRadians((357.529 + .98560028 * n) % 360)
        val q = (280.459 + .98564736 * n) % 360
        val l = Math.toRadians((q + 1.915 * sin(g) + .020 * sin(2*g)) % 360)
        val e = Math.toRadians(23.439 - .00000036*n)
        val ra = atan2(cos(e) * sin(l), cos(l))
        var eq = 4 * Math.toDegrees(ra - l)
        while (eq > 720) eq -= 1440
        while (eq < -720) eq += 1440
        return eq
    }
}

object Qibla {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl)
        val x = cos(p1) * tan(p2) - sin(p1) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}
