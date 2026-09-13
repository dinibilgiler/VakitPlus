package com.vakitplus.app

import java.time.LocalDate

object Hijri {
    private val months = arrayOf("Muharrem","Safer","Rebiülevvel","Rebiülahir","Cemaziyelevvel","Cemaziyelahir","Recep","Şaban","Ramazan","Şevval","Zilkade","Zilhicce")
    fun approximate(date: LocalDate): String {
        // Tabular approximation; replace with an authoritative calendar source for production religious-date publishing.
        val epoch = LocalDate.of(622,7,19).toEpochDay()
        val days = date.toEpochDay() - epoch
        val year = (days / 354.367).toInt() + 1
        val dayOfYear = (days - ((year-1)*354.367)).toInt().coerceAtLeast(0)
        val month = (dayOfYear / 29.53059).toInt().coerceIn(0,11)
        val day = (dayOfYear - month*29.53059).toInt().coerceIn(1,30)
        return "$day ${months[month]} ${year}"
    }
}
