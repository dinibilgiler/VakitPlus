# Vakit+ 1.6.0 — Google Play Release Checklist

## Build
- [ ] Android Studio/Gradle environment installed
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew bundleRelease`
- [ ] APK installs on a physical Android 16 device
- [ ] AAB passes Play Console pre-launch checks
- [ ] Release keystore created and backed up securely
- [ ] AAB signed with release key
- [ ] SHA-256 fingerprint recorded

## Policy / permissions
- [ ] Target SDK 36 confirmed
- [ ] POST_NOTIFICATIONS justified
- [ ] Location permissions justified
- [ ] Exact alarm permission reviewed for Play policy
- [ ] Privacy policy URL published
- [ ] Data Safety form completed
- [ ] Quran source/license attribution retained
- [ ] Adhan audio license documented before shipping audio

## Religious-data validation
- [ ] Prayer times compared against an authorized/verified source for representative dates/cities
- [ ] Hijri calendar verified against an authoritative calendar
- [ ] Qibla bearing tested on multiple devices
- [ ] Fajr/Isha calculation parameters reviewed by product owner / religious advisor
