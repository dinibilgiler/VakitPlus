package com.vakitplus.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vakitplus.app.owner.AuditEvent
import com.vakitplus.app.owner.OwnerAuditLog
import java.text.DateFormat
import java.util.Date

@Composable
fun AdminEntry(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var authenticated by remember { mutableStateOf(false) }
    var configured by remember { mutableStateOf(AdminSecurity.isConfigured(context)) }
    if (authenticated) {
        AdminDashboard(onLogout = { authenticated = false; onClose() })
    } else {
        AdminLogin(configured = configured, onAuthenticated = { authenticated = true }, onClose = onClose) {
            configured = true
        }
    }
}

@Composable
private fun AdminLogin(configured: Boolean, onAuthenticated: () -> Unit, onClose: () -> Unit, onConfigured: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Text(if (configured) "Yönetici Girişi" else "Yönetici Şifresi Oluştur", fontSize = 26.sp)
        Text(if (configured) "Vakit+ yönetim alanına yalnızca sahibi erişmelidir." else "İlk kurulumda en az 10 karakterlik güçlü bir şifre belirleyin.", modifier = Modifier.padding(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Şifre") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
        if (!configured) {
            OutlinedTextField(confirm, { confirm = it }, label = { Text("Şifre tekrar") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(10.dp))
        Button(onClick = {
            if (password.length < 10) error = "Şifre en az 10 karakter olmalı."
            else if (!configured && password != confirm) error = "Şifreler eşleşmiyor."
            else if (!configured) {
                AdminSecurity.setPassword(context, password.toCharArray()); onConfigured(); OwnerAuditLog(context).append(AuditEvent("OWNER_SETUP", "local-owner", System.currentTimeMillis(), "İlk yerel yönetici oluşturuldu")); onAuthenticated()
            } else if (AdminSecurity.verify(context, password.toCharArray())) {
                OwnerAuditLog(context).append(AuditEvent("OWNER_LOGIN", "local-owner", System.currentTimeMillis(), "Yerel yönetici girişi")); onAuthenticated()
            } else error = "Şifre hatalı."
            password = ""; confirm = ""
        }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Icon(if (configured) Icons.Default.Lock else Icons.Default.Security, null); Spacer(Modifier.width(8.dp)); Text(if (configured) "Giriş yap" else "Yönetici oluştur")
        }
        TextButton(onClick = onClose) { Text("Vazgeç") }
    }
}

@Composable
private fun AdminDashboard(onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var subtitle by remember { mutableStateOf(AdminConfig.appSubtitle(context)) }
    var daily by remember { mutableStateOf(AdminConfig.dailyMessage(context)) }
    var note by remember { mutableStateOf(AdminConfig.adminNote(context)) }
    var saved by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showRemote by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Yönetici Paneli", fontSize = 28.sp)
                    Text("Sahip / tam yetki", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Çıkış") }
            }
            Spacer(Modifier.height(18.dp))
            Text("Uygulama içeriği", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Ana sayfa alt başlığı") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(daily, { daily = it }, label = { Text("Yönetici mesajı") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3)
            OutlinedTextField(note, { note = it }, label = { Text("Yönetici notu") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3)
            Button(onClick = { AdminConfig.save(context, subtitle, daily, note); OwnerAuditLog(context).append(AuditEvent("CONTENT_UPDATE", "local-owner", System.currentTimeMillis(), "Ana sayfa içerikleri güncellendi")); saved = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Değişiklikleri kaydet")
            }
            if (saved) Text("Kaydedildi.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Text("Bildirim yönetimi", style = MaterialTheme.typography.titleMedium)
            Text("Namaz vakitleri, bildirim izinleri ve test bildirimlerini yönet.", modifier = Modifier.padding(top = 6.dp))
            Button(onClick = { showNotifications = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Bildirim Panelini Aç")
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Text("Merkezi sahip sistemi", style = MaterialTheme.typography.titleMedium)
            Text("V1.9: gerçek merkezi Owner backend bağlantısı hazır. Sunucu tarafında OWNER rolü doğrulanır; Android yalnızca oturum ve içerik senkronizasyonunu yürütür.", modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { showRemote = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Icon(Icons.Default.Security, null); Spacer(Modifier.width(8.dp)); Text("Merkezi Owner Panelini Aç")
            }
            Text("Durum: Sunucu bağlantısı yapılandırılmadı. Üretimde yetki kontrolü sunucu tarafında yapılmalıdır.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Text("Son yerel yönetim hareketleri", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp))
            OwnerAuditLog(context).recent(5).forEach { event ->
                val whenText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.timestampEpochMillis))
                ListItem({ Text(event.action) }, { Text("$whenText • ${event.details}") })
            }
            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Yönetici şifresini sıfırla") }
        }
    }
    if (showNotifications) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNotifications = false }) {
            Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                NotificationAdminPanel(onClose = { showNotifications = false })
            }
        }
    }
    if (showRemote) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showRemote = false }) {
            Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                RemoteOwnerPanel(onClose = { showRemote = false })
            }
        }
    }
    if (showReset) {
        AlertDialog(onDismissRequest = { showReset = false }, title = { Text("Şifreyi sıfırla?") }, text = { Text("Mevcut yönetici kimlik bilgisi silinecek ve bir sonraki girişte yeni şifre oluşturulacak.") }, confirmButton = { TextButton(onClick = { AdminSecurity.clear(context); OwnerAuditLog(context).append(AuditEvent("OWNER_RESET", "local-owner", System.currentTimeMillis(), "Yerel yönetici şifresi sıfırlandı")); showReset = false; onLogout() }) { Text("Sıfırla") } }, dismissButton = { TextButton(onClick = { showReset = false }) { Text("İptal") } })
    }
}
