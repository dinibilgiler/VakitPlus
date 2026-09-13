# Vakit+ — Bulut Derleme Rehberi

Firebase Studio yeni çalışma alanı kayıtlarını kapattığı için Vakit+ için GitHub Actions tabanlı derleme yolu eklenmiştir.

## Önerilen yol
1. Bu `VakitPlus` klasörünü özel bir GitHub deposuna yükleyin.
2. `Actions` sekmesinden **Vakit+ Android Build** iş akışını çalıştırın.
3. Play Store için imzalı AAB istiyorsanız aşağıdaki GitHub Secrets değerlerini ekleyin:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
4. İşlem tamamlandığında Actions > workflow run > Artifacts bölümünden `VakitPlus-release-AAB` dosyasını indirin.

## Keystore
Release keystore dosyanızı GitHub deposuna yüklemeyin. Base64 değerini yalnızca GitHub Secret olarak saklayın. Şifreleri kimseyle paylaşmayın.

## Firebase
`google-services.json` dosyasını da GitHub'a gizli/özel repo dışında koymayın. Üretim Firebase yapılandırmasını güvenli şekilde ekleyin.
