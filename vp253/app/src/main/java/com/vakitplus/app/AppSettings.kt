package com.vakitplus.app

import android.content.Context

object AppSettings {
    private const val PREF = "vakit_plus"
    private const val CITY = "city"
    private const val DARK = "dark"

    fun city(context: Context): City {
        val name = context.getSharedPreferences(PREF, 0).getString(CITY, "Batman") ?: "Batman"
        return CityDatabase.find(name) ?: BATMAN
    }
    fun saveCity(context: Context, city: City) {
        context.getSharedPreferences(PREF, 0).edit().putString(CITY, city.name).apply()
    }
    fun dark(context: Context): Boolean =
        context.getSharedPreferences(PREF, 0).getBoolean(DARK, false)
    fun saveDark(context: Context, value: Boolean) =
        context.getSharedPreferences(PREF, 0).edit().putBoolean(DARK, value).apply()
}

object CityDatabase {
    private val all = listOf(
        City("Adana",37.0,35.3213,3.0), City("Adıyaman",37.7648,38.2786,3.0),
        City("Afyonkarahisar",38.7569,30.5387,3.0), City("Ankara",39.9334,32.8597,3.0),
        City("Antalya",36.8969,30.7133,3.0), City("Aydın",37.8560,27.8416,3.0),
        City("Balıkesir",39.6484,27.8826,3.0), City("Batman",37.8812,41.1351,3.0),
        City("Bursa",40.1950,29.0600,3.0), City("Çanakkale",40.1553,26.4142,3.0),
        City("Diyarbakır",37.9144,40.2306,3.0), City("Erzurum",39.9043,41.2679,3.0),
        City("Eskişehir",39.7667,30.5256,3.0), City("Gaziantep",37.0662,37.3833,3.0),
        City("Hatay",36.2025,36.1606,3.0), City("İstanbul",41.0082,28.9784,3.0),
        City("İzmir",38.4237,27.1428,3.0), City("Kahramanmaraş",37.5858,36.9371,3.0),
        City("Kayseri",38.7205,35.4826,3.0), City("Kocaeli",40.7654,29.9408,3.0),
        City("Konya",37.8746,32.4932,3.0), City("Malatya",38.3552,38.3095,3.0),
        City("Manisa",38.6191,27.4289,3.0), City("Mardin",37.3212,40.7245,3.0),
        City("Mersin",36.8121,34.6415,3.0), City("Muğla",37.2153,28.3636,3.0),
        City("Ordu",40.9839,37.8764,3.0), City("Rize",41.0201,40.5234,3.0),
        City("Sakarya",40.7569,30.3781,3.0), City("Samsun",41.2867,36.33,3.0),
        City("Şanlıurfa",37.1674,38.7955,3.0), City("Siirt",37.9333,41.95,3.0),
        City("Sivas",39.7477,37.0179,3.0), City("Tekirdağ",40.9781,27.5110,3.0),
        City("Trabzon",41.0015,39.7178,3.0), City("Van",38.5012,43.3730,3.0)
    )
    fun all() = all
    fun find(name: String) = all.firstOrNull { it.name == name }
}
