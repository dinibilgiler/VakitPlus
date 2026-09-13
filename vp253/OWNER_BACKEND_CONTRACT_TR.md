# Vakit+ V1.8 — Merkezi Owner Backend Sözleşmesi

Bu sürüm, Android istemcisini gerçek bir merkezi sahip/yetki sistemine bağlamak için sözleşmeyi hazırlar.

## Üretim mimarisi
- `OWNER > ADMIN > EDITOR` rol hiyerarşisi.
- Yetki kontrolü sunucuda yapılır; APK içindeki kontrol yalnızca kullanıcı deneyimidir.
- Access token kısa ömürlü, refresh token döndürülerek yenilenebilir.
- Refresh token yalnızca Android Keystore ile korunan oturum deposunda tutulur.
- Kritik işlemler için yeniden kimlik doğrulama / biyometrik adım uygulanmalıdır.
- Tüm yönetim değişiklikleri audit log'a yazılır.
- Play Integrity token'ı kritik API çağrılarında sunucu tarafından doğrulanır.

## Önerilen uç noktalar
`POST /v1/auth/sign-in`
`POST /v1/auth/refresh`
`POST /v1/auth/sign-out`
`GET /v1/owner/content`
`PUT /v1/owner/content`
`POST /v1/owner/audit`

## Güvenlik kuralları
- Owner şifresi APK'ya gömülmez.
- Secret/API key kaynak koduna yazılmaz.
- HTTPS zorunludur.
- Sunucu, kullanıcının `OWNER` rolünü doğrulamadan içerik/ayar değişikliğine izin vermez.
- Play Integrity doğrulaması sunucuda yapılır; anahtar veya doğrulama sırrı istemciye konulmaz.
- Yönetici şifresi değişimi ve rol değişimi step-up authentication ister.

V1.8 Android tarafı backend'e hazırdır; gerçek merkezi yetki için bir HTTPS backend ve Google Play Console / Cloud yapılandırması gereklidir.
