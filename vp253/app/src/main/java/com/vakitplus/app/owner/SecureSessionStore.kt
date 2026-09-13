package com.vakitplus.app.owner

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Android Keystore-backed session storage. No owner password is stored here. */
class SecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("vakit_plus_owner_session", Context.MODE_PRIVATE)
    private val alias = "VakitPlusOwnerSessionKey"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(alias, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    fun save(session: OwnerSession) {
        val raw = listOf(session.userId, session.displayName, session.role.name, session.accessToken, session.refreshToken, session.expiresAtEpochSeconds).joinToString("\u001F")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(raw.toByteArray(StandardCharsets.UTF_8))
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("data", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun load(): OwnerSession? = runCatching {
        val iv = Base64.decode(prefs.getString("iv", null), Base64.NO_WRAP)
        val data = Base64.decode(prefs.getString("data", null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val p = String(cipher.doFinal(data), StandardCharsets.UTF_8).split("\u001F")
        if (p.size != 6) return null
        OwnerSession(p[0], p[1], OwnerRole.valueOf(p[2]), p[3], p[4], p[5].toLong())
    }.getOrNull()

    fun clear() { prefs.edit().clear().apply() }
}
