package com.vakitplus.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vakitplus.app.owner.OwnerContent
import com.vakitplus.app.owner.RemoteOwnerConfig
import com.vakitplus.app.owner.RemoteOwnerRepository
import kotlinx.coroutines.launch

@Composable
fun RemoteOwnerPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { RemoteOwnerRepository(context) }
    var endpoint by remember { mutableStateOf(RemoteOwnerConfig.getBaseUrl(context)) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf(repo.session()?.displayName ?: "") }
    var subtitle by remember { mutableStateOf("") }
    var daily by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationBody by remember { mutableStateOf("") }
    var notificationTopic by remember { mutableStateOf("all") }
    var scheduleMinutes by remember { mutableStateOf("0") }
    var historyText by remember { mutableStateOf("") }

    fun run(block: suspend () -> Unit) {
        loading = true; status = "İşleniyor…"
        scope.launch { try { block(); status = "İşlem başarılı." } catch (e: Exception) { status = e.message ?: "İşlem başarısız." } finally { loading = false } }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Merkezi Owner", style = MaterialTheme.typography.headlineMedium)
        Text("Sunucu tarafından doğrulanan tek sahip hesabı", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Backend HTTPS adresi") }, placeholder = { Text("https://owner.ornek.com") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { RemoteOwnerConfig.setBaseUrl(context, endpoint); status = "Sunucu adresi kaydedildi." }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Sunucu adresini kaydet") }

        if (sessionName.isBlank()) {
            OutlinedTextField(email, { email = it }, label = { Text("Owner e-posta") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Owner şifresi") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Button(enabled = !loading, onClick = { RemoteOwnerConfig.setBaseUrl(context, endpoint); run { val s=repo.login(email,password); sessionName=s.displayName; password="" } }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("Merkezi Owner girişi")
            }
        } else {
            Text("Giriş: $sessionName", modifier = Modifier.padding(top = 14.dp))
            Button(enabled = !loading, onClick = { run { val c=repo.loadContent(); subtitle=c.appSubtitle; daily=c.dailyMessage; note=c.adminNote } }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Icon(Icons.Default.Sync,null); Spacer(Modifier.width(8.dp)); Text("Merkezi içeriği getir") }
            OutlinedTextField(subtitle, { subtitle=it }, label={Text("Ana sayfa alt başlığı")}, modifier=Modifier.fillMaxWidth().padding(top=10.dp))
            OutlinedTextField(daily, { daily=it }, label={Text("Günlük / yönetici mesajı")}, minLines=3, modifier=Modifier.fillMaxWidth().padding(top=8.dp))
            OutlinedTextField(note, { note=it }, label={Text("Yönetici notu")}, minLines=3, modifier=Modifier.fillMaxWidth().padding(top=8.dp))
            Button(enabled=!loading, onClick={ run { repo.updateContent(OwnerContent(subtitle,daily,note,0)); AdminConfig.save(context,subtitle,daily,note) } }, modifier=Modifier.fillMaxWidth().padding(top=10.dp)) { Icon(Icons.Default.Save,null); Spacer(Modifier.width(8.dp)); Text("Merkeze kaydet") }
            HorizontalDivider(Modifier.padding(vertical = 18.dp))
            Text("Merkezi bildirim gönder", style = MaterialTheme.typography.titleMedium)
            Text("Bu bölüm Firebase Cloud Messaging yapılandırılmış üretim sunucusuna bildirim gönderir.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(notificationTitle, { notificationTitle=it }, label={Text("Bildirim başlığı")}, singleLine=true, modifier=Modifier.fillMaxWidth().padding(top=8.dp))
            OutlinedTextField(notificationBody, { notificationBody=it }, label={Text("Bildirim metni")}, minLines=3, modifier=Modifier.fillMaxWidth().padding(top=8.dp))
            OutlinedTextField(notificationTopic, { notificationTopic=it }, label={Text("Hedef konu")}, placeholder={Text("all / batman / imamlar vb.")}, singleLine=true, modifier=Modifier.fillMaxWidth().padding(top=8.dp))
            OutlinedTextField(scheduleMinutes, { scheduleMinutes=it.filter(Char::isDigit) }, label={Text("Kaç dakika sonra? (0 = hemen)")}, singleLine=true, modifier=Modifier.fillMaxWidth().padding(top=8.dp))
            Row(Modifier.fillMaxWidth().padding(top=10.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(enabled=!loading && notificationTitle.isNotBlank() && notificationBody.isNotBlank(), onClick={ run { val mins=scheduleMinutes.toLongOrNull() ?: 0L; repo.sendNotification(com.vakitplus.app.owner.NotificationCampaign(notificationTitle, notificationBody, notificationTopic.ifBlank { "all" }, if(mins>0) System.currentTimeMillis()+mins*60000 else null)); notificationTitle=""; notificationBody="" } }, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Send,null); Spacer(Modifier.width(6.dp)); Text(if((scheduleMinutes.toLongOrNull()?:0)>0) "Zamanla" else "Hemen gönder") }
                OutlinedButton(
                    enabled = !loading,
                    onClick = {
                        run {
                            val history = repo.notificationHistory()
                            val sentCount = history.first.size
                            val scheduledCount = history.second.count { item -> item.status == "SCHEDULED" }
                            val recent = history.first.take(8).joinToString("\n") { record ->
                                "${record.topic} — ${record.title}"
                            }
                            historyText = "Gönderilen: $sentCount • Zamanlanan: $scheduledCount\n$recent"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.History, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Geçmiş")
                }
            }
            OutlinedButton(
                enabled = !loading,
                onClick = {
                    run {
                        val stats = repo.notificationStats()
                        historyText = "Toplam gönderim: ${stats.totalSent}\n" +
                            "Zamanlanan: ${stats.totalScheduled}\n" +
                            "Taslak: ${stats.totalDrafts}\n" +
                            "Konu dağılımı: ${stats.byTopic}"
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Bildirim istatistikleri")
            }
            if(historyText.isNotBlank()) Text(historyText, style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(top=10.dp))
            Text("Hedefleme: FCM konu aboneliği kullanan cihazlara gönderilir. FCM konu mesajları sunucu tarafından Admin SDK/HTTP v1 ile gönderilebilir.", style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(top=10.dp))
            OutlinedButton(onClick={repo.logout();sessionName="";status="Merkezi oturum kapatıldı."}, modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("Merkezi oturumu kapat")}
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=12.dp))
        if (status.isNotBlank()) Text(status, modifier=Modifier.padding(top=10.dp))
        Spacer(Modifier.height(10.dp))
        Text("Güvenlik: Owner parolası uygulamada saklanmaz. Oturum belirteçleri Android Keystore ile şifrelenir. Üretim sunucusunda HTTPS zorunludur.", style=MaterialTheme.typography.bodySmall)
        TextButton(onClick=onClose, modifier=Modifier.padding(top=8.dp)){Text("Kapat")}
    }
}
