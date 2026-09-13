# Vakit+ Merkezi Owner Backend

Bu servis Vakit+ uygulamasındaki tek Owner hesabını sunucu tarafında yetkilendirmek için hazırlanmıştır.

## Çalıştırma

Node.js 20+ gerekir.

1. `.env.example` dosyasını `.env` olarak kopyalayın.
2. `OWNER_EMAIL`, `OWNER_PASSWORD` ve en az 32 karakterlik rastgele `JWT_SECRET` belirleyin.
3. Ortam değişkenlerini işletim sistemi/hosting panelinden verin.
4. `node server.js` çalıştırın.
5. Üretimde HTTPS'i reverse proxy/hosting katmanında zorunlu tutun.

## Uç noktalar

- `GET /health`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `POST /v1/auth/logout`
- `GET /v1/content`
- `PUT /v1/content`
- `GET /v1/audit`
- `POST /v1/audit`

## Önemli üretim notu

Bu başlangıç sürümü küçük/tek-owner kurulum için dosya tabanlı veri saklar. Çok kullanıcılı veya yüksek trafikli üretimde PostgreSQL/Firestore gibi yönetilen bir veritabanına geçirilmelidir. TLS olmadan internete açılmamalıdır. JWT secret ve Owner parolası kaynak koda yazılmamalıdır.

Play Integrity doğrulaması da backend'de yapılmalıdır; istemci tarafından gönderilen bir integrity bilgisini tek başına güvenilir kabul etmeyin.
