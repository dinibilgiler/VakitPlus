# Vakit+ V1.5 — Professional UI/UX

V1.4 bildirim altyapısının devamıdır.

## V1.5
- Premium Material 3 arayüz iyileştirmeleri
- Ana sayfada gerçek zamanlı sonraki namaz geri sayımı
- Koyu temada daha doğru yüzey/metin kontrastı
- İlk açılış bilgilendirme/onboarding diyaloğu
- Kıble ekranında kalibrasyon ve kullanım yönlendirmesi
- Uygulama ikonu
- Tab durumunun ekran yeniden oluşturulmalarında korunması
- SCHEDULE_EXACT_ALARM + exact mümkün değilse inexact fallback
- 1.5.0 / versionCode 15
- API 36 hedefi korunuyor

## Derleme doğrulaması
Bu çalışma ortamında Gradle/Android SDK build toolchain mevcut olmadığı için gerçek `assembleDebug` veya `bundleRelease` çalıştırılamadı. Bunun yerine V1.5 için kaynak/manifest/assets statik doğrulaması `VALIDATE.sh` ile yapılmıştır.

Google Play açısından `USE_EXACT_ALARM` yalnızca temel işlevi hassas zamanlama gerektiren sınırlı uygulama kategorileri için uygundur. Vakit+ için yayın öncesi değerlendirmede `SCHEDULE_EXACT_ALARM` yaklaşımı tercih edilmiştir; Android exact alarm dokümantasyonu da iki iznin farklı kullanım/izin modelleri olduğunu belirtir.

## Üretim öncesi kalan kritik işler
- Yetkili/doğrulanmış namaz vakti veri kaynağı veya doğrulanmış hesaplama standardı
- Hicri tarihin yetkili takvimle doğrulanması
- Lisanslı ezan sesi
- Gerçek cihazlarda sensör/konum/bildirim testi
- Release signing + AAB
- Play Console Data Safety/KVKK/gizlilik belgeleri


## V2.3
Gelişmiş içerik ve bildirim merkezi: zamanlama, hedefleme, geçmiş ve istatistik.
