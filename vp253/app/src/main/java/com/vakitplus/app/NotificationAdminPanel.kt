package com.vakitplus.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vakitplus.app.owner.RemoteOwnerRepository
import kotlinx.coroutines.launch

@Composable
fun NotificationAdminPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val names = NotificationSettings.names()
    var refresh by remember { mutableIntStateOf(0) }
    var testTitle by remember { mutableStateOf("Vakit+ Test Bildirimi") }
    var testBody by remember { mutableStateOf("Yönetici panelinden test bildirimi.") }
    var status by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item {
            Text("Bildirim Yönetimi", style = MaterialTheme.typography.headlineMedium)
            Text("Namaz vakitleri ve yönetici bildirimlerini buradan kontrol edin.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            val granted = android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ListItem({ Text("Sistem bildirim izni") }, { Text(if (granted) "Açık" else "Kapalı") })
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Android bildirim ayarlarını aç") }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Namaz vakti bildirimleri", style = MaterialTheme.typography.titleMedium)
        }
        items(names.size) { index ->
            val name = names[index]
            var enabled by remember(refresh) { mutableStateOf(NotificationSettings.enabled(context, name)) }
            var before by remember(refresh) { mutableIntStateOf(NotificationSettings.beforeMinutes(context, name)) }
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Switch(checked = enabled, onCheckedChange = { enabled = it; NotificationSettings.save(context, name, enabled, before); NotificationScheduler.scheduleTomorrowFromSavedCity(context) })
                    }
                    Text("Önceden: ${if (before == 0) "Tam vaktinde" else "$before dk"}")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        NotificationSettings.offsets().forEachIndexed { i, value ->
                            SegmentedButton(selected = before == value, onClick = { before = value; NotificationSettings.save(context, name, enabled, before); NotificationScheduler.scheduleTomorrowFromSavedCity(context) }, shape = SegmentedButtonDefaults.itemShape(i, NotificationSettings.offsets().size)) { Text(value.toString()) }
                        }
                    }
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 18.dp))
            Text("Test bildirimi", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(testTitle, { testTitle = it }, label = { Text("Başlık") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(testBody, { testBody = it }, label = { Text("Mesaj") }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Button(onClick = { LocalNotificationHelper.show(context, testTitle, testBody); status = "Test bildirimi gönderildi." }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("Cihaza test bildirimi gönder")
            }
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Kapat") }
        }
    }
}

object LocalNotificationHelper {
    private const val CHANNEL = "admin_test"
    fun show(context: Context, title: String, body: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(android.app.NotificationChannel(CHANNEL, "Yönetici Test", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL).setSmallIcon(com.vakitplus.app.R.drawable.ic_vakitplus).setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        nm.notify(9001, notification)
    }
}
