package com.vakitplus.app

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private val Navy = Color(0xFF102A43)
private val Green = Color(0xFF176B52)
private val Surface = Color(0xFFF6F8F5)

data class City(val name: String, val lat: Double, val lon: Double, val tz: Double)
val BATMAN = City("Batman", 37.8812, 41.1351, 3.0)
val ISTANBUL = City("İstanbul", 41.0082, 28.9784, 3.0)
val ANKARA = City("Ankara", 39.9334, 32.8597, 3.0)
val cities get() = CityDatabase.all()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationScheduler.createChannel(this)
        runCatching {
            FirebaseApp.initializeApp(this)
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        runCatching { com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all") }
        QuranRepository.load(this)
        setContent { VakitPlusApp() }
    }
}

@Composable
fun VakitPlusApp() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showWelcome by rememberSaveable { mutableStateOf(!context.getSharedPreferences("vakit_plus", Context.MODE_PRIVATE).getBoolean("welcome_seen", false)) }
    var city by remember { mutableStateOf(AppSettings.city(context)) }
    var dark by remember { mutableStateOf(AppSettings.dark(context)) }
    var locationText by remember { mutableStateOf("Konum bekleniyor…") }
    var locationError by remember { mutableStateOf(false) }
    var showAdmin by rememberSaveable { mutableStateOf(false) }
    var showDashboard by rememberSaveable { mutableStateOf(false) }

    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) locationError = true
    }
    LaunchedEffect(Unit) {
        request.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
        val loc = AppLocation.getLastKnown(context)
        if (loc != null) {
            val nearest = CityDatabase.all().minByOrNull { (it.lat - loc.latitude) * (it.lat - loc.latitude) + (it.lon - loc.longitude) * (it.lon - loc.longitude) }
            if (nearest != null) {
                city = nearest
                AppSettings.saveCity(context, nearest)
                locationText = "Konum bulundu • ${nearest.name}"
            }
        } else locationText = "Kayıtlı konum bulunamadı • ${city.name} kullanılıyor"
    }

    val colors = if (dark) darkColorScheme(primary = Color(0xFF5FD3AA), secondary = Color(0xFF9FC3DD))
    else lightColorScheme(primary = Green, secondary = Navy, background = Surface)

    MaterialTheme(colorScheme = colors) {
        if (showWelcome) {
            AlertDialog(
                onDismissRequest = {
                    context.getSharedPreferences("vakit_plus", Context.MODE_PRIVATE).edit().putBoolean("welcome_seen", true).apply()
                    showWelcome = false
                },
                title = { Text("Vakit+’a hoş geldiniz") },
                text = { Text("Namaz vakitleri, kıble, Kur’an ve günlük içerik tek yerde. Konum ve bildirim izinleri yalnızca ilgili özellikler için kullanılır.") },
                confirmButton = {
                    TextButton(onClick = {
                        context.getSharedPreferences("vakit_plus", Context.MODE_PRIVATE).edit().putBoolean("welcome_seen", true).apply()
                        showWelcome = false
                    }) { Text("Başla") }
                }
            )
        }
        if (showDashboard) {
            OwnerDashboard { showDashboard = false }
        } else if (showAdmin) {
            AdminEntry { showAdmin = false }
            return@MaterialTheme
        }
        Scaffold(bottomBar = {
            NavigationBar {
                val labels = listOf("Ana Sayfa", "Vakitler", "Kıble", "Kur'an", "Ayarlar")
                val icons = listOf(Icons.Default.Home, Icons.Default.Schedule, Icons.Default.Explore, Icons.Default.MenuBook, Icons.Default.Settings)
                labels.forEachIndexed { i, label ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icons[i], null) }, label = { Text(label, fontSize = 10.sp) })
                }
            }
        }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> Home(city, locationText)
                    1 -> Times(city)
                    2 -> Qibla(city)
                    3 -> Quran()
                    else -> SettingsPage(city, dark, locationText, locationError, onAdmin = { showAdmin = true }, onDashboard = { showDashboard = true }) { newCity, newDark ->
                        city = newCity
                        dark = newDark
                        AppSettings.saveCity(context, newCity)
                        AppSettings.saveDark(context, newDark)
                    }
                }
            }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String) {
    Column(Modifier.padding(20.dp)) {
        Text(title, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Home(city: City, locationText: String) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val t = PrayerEngine.calculate(today, city.lat, city.lon, city.tz)
    val list = t.asList()
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }
    val nextIndex = list.indexOfFirst { runCatching { LocalTime.parse(it.second) }.getOrNull()?.isAfter(now) == true }
    val next = if (nextIndex >= 0) list[nextIndex] else list.first()
    val nextTime = runCatching { LocalTime.parse(next.second) }.getOrNull() ?: now
    val secondsUntil = if (nextIndex >= 0) java.time.Duration.between(now, nextTime).seconds.coerceAtLeast(0) else java.time.Duration.between(now, nextTime.plusHours(24)).seconds.coerceAtLeast(0)
    val countdown = "%02d:%02d:%02d".format(secondsUntil / 3600, (secondsUntil % 3600) / 60, secondsUntil % 60)
    val daily = QuranRepository.dailyAyah(today.dayOfYear)
    val hadith = QuranRepository.dailyHadith(today.dayOfYear)
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Header("Vakit+", "${AdminConfig.appSubtitle(context)} • ${city.name} • $today")
            Text(locationText, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = Green, fontSize = 12.sp)
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Column(Modifier.padding(22.dp)) {
                    Text("SONRAKİ NAMAZ", color = Color.White.copy(.65f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(next.first, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(next.second, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(countdown, color = Color(0xFFB9E6D4), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sonraki vakte kalan süre", color = Color.White.copy(.75f), fontSize = 12.sp)
                    Text("Vakitler cihazın yerel saat dilimine göre hesaplanır.", color = Color.White.copy(.7f), fontSize = 12.sp)
                }
            }
            list.forEach { (name, time) ->
                ListItem({ Text(name, fontWeight = FontWeight.SemiBold) }, { Text(time, color = Navy, fontWeight = FontWeight.Bold) }, { Icon(Icons.Default.Schedule, null, tint = Green) })
                HorizontalDivider()
            }
            ContentCard("Günün Ayeti", daily.text, "${daily.surah}:${daily.number}")
            ContentCard("Günün Hadisi", hadith.text, hadith.source)
            if (AdminConfig.dailyMessage(context).isNotBlank()) ContentCard("Yönetici Mesajı", AdminConfig.dailyMessage(context), "Vakit+ Kontrol Paneli")
            OutlinedButton(onClick = { NotificationScheduler.scheduleNextDay(context, t) }, Modifier.padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Yarının bildirimlerini kur")
            }
        }
    }
}

@Composable
fun ContentCard(title: String, body: String, source: String) {
    Card(Modifier.padding(16.dp).fillMaxWidth(), RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = Green, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
            Spacer(Modifier.height(5.dp))
            Text(source, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun Times(city: City) {
    val t = PrayerEngine.calculate(LocalDate.now(), city.lat, city.lon, city.tz)
    Column {
        Header("Vakitler", "${city.name} • ${LocalDate.now()}")
        t.asList().forEach { (n, v) ->
            ListItem({ Text(n, fontWeight = FontWeight.SemiBold) }, { Text(v, color = Navy, fontWeight = FontWeight.Bold) }, { Icon(Icons.Default.WbSunny, null, tint = Green) })
            HorizontalDivider()
        }
        Text("Bu sürümde vakitler yerel astronomik motorla hesaplanır. Resmî Diyanet veri kaynağı entegrasyonu yayın öncesinde ayrıca doğrulanmalıdır.", Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun Qibla(city: City) {
    val context = LocalContext.current
    val target = Qibla.bearing(city.lat, city.lon)
    var heading by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val r = FloatArray(9); SensorManager.getRotationMatrixFromVector(r, e.values)
                val o = FloatArray(3); SensorManager.getOrientation(r, o)
                heading = ((Math.toDegrees(o[0].toDouble()).toFloat() + 360) % 360)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sm.unregisterListener(listener) }
    }
    val relative = ((target - heading + 540) % 360) - 180
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("Kıble Pusulası", city.name)
        Box(Modifier.size(285.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Explore, null, tint = Green, modifier = Modifier.size(110.dp))
                Text("${target.roundToInt()}°", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(if (abs(relative) < 8) "Kıble yönündesiniz" else "Telefonu kıbleye çevirin", color = Color.Gray)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Telefon yönü: ${heading.roundToInt()}° • Kıble: ${target.roundToInt()}°", color = MaterialTheme.colorScheme.onBackground)
        Text("Daha hassas sonuç için telefonu düz tutun ve pusulayı kalibre edin.", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
fun Quran() {
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(24f) }
    val context = LocalContext.current

    if (selectedSurah != null) {
        QuranReader(selectedSurah!!, fontSize, { fontSize = it }, { selectedSurah = null })
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Kur'an-ı Kerim", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("114 sure • çevrimdışı okuma", fontSize = 14.sp, color = Color.Gray)
            }
            IconButton(onClick = { searchMode = !searchMode }) { Icon(Icons.Default.Search, "Ara") }
        }
        if (searchMode) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), label = { Text("Ayetlerde ara") }, singleLine = true, trailingIcon = {
                if (query.isNotBlank()) IconButton({ query = "" }) { Icon(Icons.Default.Clear, null) }
            })
        }
        if (query.isNotBlank()) {
            val results = QuranRepository.search(query)
            LazyColumn { items(results, key = { "${it.surah}:${it.number}" }) { ayah ->
                val surah = QuranRepository.allSurahs().first { it.number == ayah.surah }
                ListItem({ Text(ayah.text, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }, { Text("${surah.name} ${ayah.number}", color = Green) })
                HorizontalDivider()
            } }
        } else {
            LazyColumn {
                items(QuranRepository.allSurahs(), key = { it.number }) { s ->
                    ListItem({ Text("${s.number}. ${s.name}", fontWeight = FontWeight.SemiBold) }, { Text("${s.ayahCount} ayet", color = Color.Gray) }, { Icon(Icons.Default.MenuBook, null, tint = Green) }, { Icon(Icons.Default.ChevronRight, null) })
                    HorizontalDivider()
                }
            }
        }
    }
    LaunchedEffect(Unit) { QuranRepository.load(context) }
}

@Composable
fun QuranReader(surah: Surah, fontSize: Float, onFontSize: (Float) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val ayahs = remember(surah.number) { QuranRepository.ayahsForSurah(surah.number) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text(surah.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("${surah.number}. sure • ${surah.ayahCount} ayet", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = { onFontSize((fontSize - 1).coerceAtLeast(18f)) }) { Icon(Icons.Default.TextDecrease, null) }
            IconButton(onClick = { onFontSize((fontSize + 1).coerceAtMost(34f)) }) { Icon(Icons.Default.TextIncrease, null) }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
            items(ayahs, key = { "${it.surah}:${it.number}" }) { ayah ->
                val marked = QuranBookmarks.contains(context, ayah)
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${ayah.number}", color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { QuranBookmarks.toggle(context, ayah) }) {
                                Icon(if (marked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Yer imi", tint = Green)
                            }
                        }
                        Text(ayah.text, fontSize = fontSize.sp, lineHeight = (fontSize * 1.75f).sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPage(city: City, dark: Boolean, locationText: String, locationError: Boolean, onAdmin: () -> Unit, onDashboard: () -> Unit, onChange: (City, Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    Column {
        Header("Ayarlar", "Vakit+ V1.4")
        ListItem({ Text("Otomatik konum") }, { Text(locationText) }, { Icon(Icons.Default.MyLocation, null, tint = Green) })
        if (locationError) Text("Konum izni verilmedi. Manuel şehir seçimi kullanılabilir.", Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
        Box(Modifier.fillMaxWidth()) {
            ListItem({ Text("Şehir") }, { Text(city.name) }, { Icon(Icons.Default.LocationOn, null, tint = Green) }, { TextButton({ expanded = true }) { Text("Değiştir") } })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CityDatabase.all().forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { onChange(c, dark); expanded = false; NotificationScheduler.scheduleTomorrowFromSavedCity(context) }) }
            }
        }
        HorizontalDivider()
        ListItem({ Text("Koyu tema") }, { Text(if (dark) "Açık" else "Kapalı") }, { Icon(Icons.Default.DarkMode, null, tint = Green) }, { Switch(dark, { onChange(city, it) }) })
        HorizontalDivider()
        NotificationPreferences(refresh) { refresh++ }
        HorizontalDivider()
        ListItem({ Text("Kur'an") }, { Text("114 sure • Arapça metin • çevrimdışı") }, { Icon(Icons.Default.MenuBook, null, tint = Green) })
        HorizontalDivider()
        ListItem({ Text("Yer imleri") }, { Text("Kaydedilen ayetler cihazda tutulur") }, { Icon(Icons.Default.Bookmark, null, tint = Green) })
        HorizontalDivider()
        ListItem({ Text("Vakit yöntemi") }, { Text("Astronomik • Fajr 18° • Isha 17° • Hanafi Asr") }, { Icon(Icons.Default.Tune, null, tint = Green) })
        HorizontalDivider()
        Text("Kur'an metni: Tanzil Project kaynaklı Uthmani metin. Kaynak ve lisans bilgisi proje README'sinde yer alır.", Modifier.padding(16.dp), color = Color.Gray, fontSize = 11.sp)
        OutlinedButton(onClick = onDashboard, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Icon(Icons.Default.Dashboard, null)
            Spacer(Modifier.width(8.dp))
            Text("Merkezi Owner Dashboard")
        }
        OutlinedButton(onClick = onAdmin, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(Icons.Default.AdminPanelSettings, null)
            Spacer(Modifier.width(8.dp))
            Text("Yönetici / Kontrol Paneli")
        }
    }
}

@Composable
fun NotificationPreferences(refresh: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Namaz Bildirimleri", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Green, fontWeight = FontWeight.Bold)
        Text("Vakit+ bildirimleri cihazınızda planlanır. Android 13 ve üzeri için bildirim izni gereklidir.", Modifier.padding(horizontal = 16.dp), color = Color.Gray, fontSize = 12.sp)
        NotificationSettings.names().forEach { name ->
            key(refresh, name) {
                var enabled by remember { mutableStateOf(NotificationSettings.enabled(context, name)) }
                var before by remember { mutableIntStateOf(NotificationSettings.beforeMinutes(context, name)) }
                ListItem(
                    headlineContent = { Text(name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(if (before == 0) "Vakit saatinde" else "$before dakika önce") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (enabled) {
                                var menu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { menu = true }) { Text(if (before == 0) "Tam vakit" else "$before dk") }
                                    DropdownMenu(menu, { menu = false }) {
                                        NotificationSettings.offsets().forEach { offset ->
                                            DropdownMenuItem(text = { Text(if (offset == 0) "Tam vakit" else "$offset dakika önce") }, onClick = {
                                                before = offset
                                                NotificationSettings.save(context, name, enabled, before)
                                                NotificationScheduler.scheduleTomorrowFromSavedCity(context)
                                                menu = false
                                                onChanged()
                                            })
                                        }
                                    }
                                }
                            }
                            Switch(checked = enabled, onCheckedChange = {
                                enabled = it
                                NotificationSettings.save(context, name, enabled, before)
                                NotificationScheduler.scheduleTomorrowFromSavedCity(context)
                                onChanged()
                            })
                        }
                    }
                )
            }
        }
        OutlinedButton(onClick = {
            NotificationScheduler.scheduleTomorrowFromSavedCity(context)
            onChanged()
        }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Bildirimleri yeniden planla")
        }
    }
}
