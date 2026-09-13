package com.vakitplus.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.vakitplus.app.owner.RemoteOwnerRepository

@Composable
fun OwnerDashboard(onBack: () -> Unit) {
    val context= LocalContext.current
    val repo= remember { RemoteOwnerRepository(context) }
    var text by remember { mutableStateOf("Panel verilerini yüklemek için yenileye basın.") }
    var announcement by remember { mutableStateOf("") }
    var maintenance by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    fun refresh(){ loading=true; try { val s=repo.dashboardSummary(); maintenance=s.maintenance; text="Cihazlar: ${s.devices}\nAktif son 7 gün: ${s.activeDevices}\nBildirimleri açık: ${s.notificationsEnabled}\nGönderilen bildirim: ${s.notificationsSent}\nZamanlanan: ${s.scheduledNotifications}\nTaslak: ${s.drafts}\nAudit kaydı: ${s.auditEvents}"; announcement=repo.getSystemSettings().announcement } catch(e:Exception){ text="Hata: ${e.message}" } finally { loading=false } }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Owner Dashboard",style=MaterialTheme.typography.headlineMedium)
        Text("Vakit+ merkezi yönetim özeti",color=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Button(enabled=!loading,onClick={refresh},modifier=Modifier.fillMaxWidth()){Text("Verileri yenile")}
        Card(Modifier.fillMaxWidth().padding(top=12.dp)){Column(Modifier.padding(16.dp)){Text("Sistem Özeti",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text(text)}}
        Spacer(Modifier.height(16.dp))
        Text("Bakım modu",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(maintenance) "Aktif" else "Kapalı");Switch(checked=maintenance,onCheckedChange={maintenance=it})}
        OutlinedTextField(announcement,{announcement=it},label={Text("Merkezi duyuru")},minLines=2,modifier=Modifier.fillMaxWidth())
        Button(enabled=!loading,onClick={loading=true;try{repo.updateSystemSettings(maintenance,announcement);text="Sistem ayarları kaydedildi."}catch(e:Exception){text="Hata: ${e.message}"}finally{loading=false}},modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Text("Sistem ayarlarını kaydet")}
        Spacer(Modifier.height(12.dp))
        Text("Yetki: OWNER • Bu ekran merkezi sunucu yetkisiyle çalışır.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick=onBack){Text("Geri")}
    }
}
