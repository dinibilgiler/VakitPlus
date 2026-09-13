# Vakit+ — Android Studio olmadan derleme seçenekleri

## 1. Önerilen: Android Studio Cloud / Firebase Studio
Tarayıcıdan bulut Linux VM üzerinde Android Studio projesini açıp SDK bileşenleriyle çalışabilirsiniz.

## 2. GitHub Codespaces
Kaynak kodu GitHub'a koyup tarayıcıdan Codespace açılabilir. Android derleme araçları kurulabilir; ancak Android emülatörü ve USB cihaz bağlantısı Codespaces içinde uygun değildir. APK/AAB üretimi için CI daha uygundur.

## 3. GitHub Actions
Kaynak GitHub'a alındığında Gradle build'i bulut CI üzerinde çalıştırılabilir. Release signing için keystore ve secret'lar GitHub Secrets'ta tutulmalıdır.

## 4. Codemagic / Bitrise gibi CI servisleri
Android projesi bağlanıp imzalı APK/AAB üretilebilir. Üretim imzalama anahtarı yalnızca secret store'da tutulmalıdır.
