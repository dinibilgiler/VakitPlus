# Vakit+ V1.9

V1.9, Vakit+ uygulamasını yerel yönetici panelinden merkezi Owner mimarisine taşıyan sürümdür.

## Paket içeriği
- `app/`: Android V1.9 kaynak kodu
- `backend/`: Node.js 20+ Owner backend başlangıç servisi
- `V1.9_DEPLOYMENT_TR.md`: kurulum/yayınlama sırası
- `OWNER_BACKEND_CONTRACT_TR.md`: API sözleşmesi

## Mimari
`Vakit+ Android → HTTPS → Owner Backend → içerik/audit depolama`

Android uygulaması Owner kararını kendi içinde üretmez. Merkezi backend OWNER rolünü belirler.

## Üretim uyarısı
Backend örneği bilinçli olarak küçük tutulmuştur. İnternete doğrudan HTTP ile açılmamalı; HTTPS reverse proxy kullanılmalı ve üretimde veritabanı, rate limiting, yedekleme, monitoring ve Play Integrity doğrulaması eklenmelidir.
