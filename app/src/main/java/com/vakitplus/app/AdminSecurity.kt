package com.vakitplus.app

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local owner gate for app-level administration.
 * Production note: critical remote operations must also be authorized by a server.
 */
object AdminSecurity {
    private const val PREF = "vakit_plus_admin"
    private const val SALT = "salt"
    private const val HASH = "hash"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isConfigured(context: Context): Boolean =
        context.getSharedPreferences(PREF, 0).contains(HASH)

    fun setPassword(context: Context, password: CharArray) {
        require(password.size >= 10) { "Şifre en az 10 karakter olmalı." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        context.getSharedPreferences(PREF, 0).edit()
            .putString(SALT, salt.toHex())
            .putString(HASH, hash.toHex())
            .apply()
        password.fill('\u0000')
    }

    fun verify(context: Context, password: CharArray): Boolean {
        val prefs = context.getSharedPreferences(PREF, 0)
        val salt = prefs.getString(SALT, null)?.hexToBytes() ?: return false
        val expected = prefs.getString(HASH, null)?.hexToBytes() ?: return false
        val actual = derive(password, salt)
        password.fill('\u0000')
        return MessageDigest.isEqual(expected, actual)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, 0).edit().clear().apply()
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray = ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
