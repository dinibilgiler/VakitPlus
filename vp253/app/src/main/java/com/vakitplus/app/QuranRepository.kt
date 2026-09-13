package com.vakitplus.app

import android.content.Context
import org.json.JSONArray

data class Surah(val number: Int, val name: String, val ayahCount: Int)
data class Ayah(val surah: Int, val number: Int, val text: String)

data class Hadith(val text: String, val source: String)

object QuranRepository {
    private var loaded = false
    private val surahs = mutableListOf<Surah>()
    private val ayahs = mutableListOf<Ayah>()
    private val rawVerseRegex = Regex("\\\\qt@no\\{([^}]*)\\}")

    fun load(context: Context) {
        if (loaded) return
        val json = JSONArray(context.assets.open("surahs.json").bufferedReader().use { it.readText() })
        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            surahs += Surah(o.getInt("no"), o.getString("name"), o.getInt("ayahCount"))
        }
        val lines = context.assets.open("quran_uthmani_source.txt").bufferedReader().readLines()
        var surahIndex = 0
        var ayahInSurah = 0
        for (line in lines) {
            val clean = line.replace("\\basmalah", "").trim()
            val marker = rawVerseRegex.find(clean)
            val text = clean.replace(rawVerseRegex, "").trim()
            ayahInSurah++
            if (surahIndex < surahs.size) {
                ayahs += Ayah(surahs[surahIndex].number, ayahInSurah, text)
                if (ayahInSurah >= surahs[surahIndex].ayahCount) {
                    surahIndex++
                    ayahInSurah = 0
                }
            }
        }
        loaded = true
    }

    fun allSurahs(): List<Surah> = surahs
    fun ayahsForSurah(number: Int): List<Ayah> = ayahs.filter { it.surah == number }
    fun search(query: String): List<Ayah> = if (query.isBlank()) emptyList() else ayahs.filter { it.text.contains(query, ignoreCase = true) }.take(100)
    fun dailyAyah(dayOfYear: Int): Ayah = ayahs[(dayOfYear - 1) % ayahs.size]

    fun dailyHadith(dayOfYear: Int): Hadith = listOf(
        Hadith("Ameller niyetlere göredir. Her kişiye niyet ettiği şey vardır.", "Buhârî, Bed'ü'l-Vahy, 1; Müslim, İmâre, 155"),
        Hadith("Müslüman, elinden ve dilinden Müslümanların emin olduğu kimsedir.", "Buhârî, Îmân, 4; Müslim, Îmân, 64"),
        Hadith("Hiçbir anne baba, çocuğuna güzel terbiyeden daha kıymetli bir bağışta bulunmamıştır.", "Tirmizî, Birr, 33")
    )[dayOfYear % 3]
}

object QuranBookmarks {
    private const val PREF = "quran_bookmarks"
    private const val KEY = "items"
    fun get(context: Context): Set<String> = context.getSharedPreferences(PREF, 0).getStringSet(KEY, emptySet()) ?: emptySet()
    fun toggle(context: Context, ayah: Ayah): Boolean {
        val prefs = context.getSharedPreferences(PREF, 0)
        val set = get(context).toMutableSet()
        val key = "${ayah.surah}:${ayah.number}"
        val added = set.add(key).not()
        if (!added) set.remove(key)
        prefs.edit().putStringSet(KEY, set).apply()
        return !added
    }
    fun contains(context: Context, ayah: Ayah): Boolean = get(context).contains("${ayah.surah}:${ayah.number}")
}
