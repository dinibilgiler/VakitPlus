# Vakit+ V1.7 Yönetici / Kontrol Paneli

## Kullanım
- Ayarlar > Yönetici / Kontrol Paneli.
- İlk girişte yönetici şifresi oluşturulur (en az 10 karakter).
- Sonraki girişlerde şifre doğrulanır.
- Yönetici mesajı, ana sayfa alt başlığı ve yönetici notu değiştirilebilir.
- Şifre sıfırlama işlemi panel içinden yapılabilir.

## Güvenlik mimarisi
Şifre düz metin olarak tutulmaz. Rastgele salt + PBKDF2-HMAC-SHA256 ile türetilmiş özet tutulur.

Ancak bu panel **yerel cihaz yönetimi** içindir. APK içindeki herhangi bir yerel yetkilendirme, tersine mühendislik veya değiştirilmiş APK ile tamamen güvenli bir sunucu yetkilendirmesinin yerini tutamaz. OWASP, kritik yetkilendirmenin uzak uçta da uygulanmasını önerir.

Üretim aşamasında gerçek "sadece ben yetkiliyim" modeli için önerilen yapı:
1. Sunucu tarafında Owner hesabı.
2. Kısa ömürlü erişim tokenı + refresh token.
3. Rol tabanlı yetkilendirme: OWNER > ADMIN > EDITOR.
4. HTTPS/TLS.
5. Audit log.
6. Play Integrity API ile riskli/tahrif edilmiş istemci kontrolü.
7. Kritik değişikliklerde yeniden kimlik doğrulama / biyometri.

Kaynaklar: Android Security Best Practices ve OWASP MASVS-AUTH / MASVS-STORAGE.
