# Vakit+ 1.6.0 Build Verification Report

Date: 2026-09-13

## Environment
- Java detected: OpenJDK 21
- Gradle executable: NOT AVAILABLE
- Android SDK / sdkmanager: NOT AVAILABLE
- adb: NOT AVAILABLE
- aapt2: NOT AVAILABLE

## Result
A true Android APK/AAB build could NOT be executed in this environment because the Android SDK/build toolchain is unavailable and external distribution downloads are unavailable.

Therefore this package is NOT labeled as a verified APK/AAB build.

## Static checks performed
- compileSdk = 36
- targetSdk = 36
- minSdk = 26
- versionCode = 16
- versionName = 1.6.0
- Android applicationId = com.vakitplus.app
- source/assets/package structure present
- release build type present

## Required local verification
Run in Android Studio or an environment with Android SDK Platform 36 and Build Tools installed:

`./gradlew assembleDebug`
`./gradlew bundleRelease`

Then install the debug APK on a physical device and upload the signed AAB to Play Console internal testing.
