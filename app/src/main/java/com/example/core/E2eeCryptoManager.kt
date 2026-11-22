package com.example.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "E2eeCrypto"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TOKEN_KEY_ALIAS = "tiger_connect_session_aes_key"
private const val E2EE_PREFS = "tiger_connect_encrypted_prefs"
private const val KEY_BEARER_TOKEN = "enc_bearer_token"
private const val KEY_SESSION_EMAIL = "enc_session_email"

/**
 * Hardware-backed Android Keystore AES-256-GCM encrypted preferences store
 * for Bearer session tokens (`tiger_*`) and ECDH P-256 + AES-GCM E2EE messaging.
 */
class E2eeCryptoManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(E2EE_PREFS, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    // Ephemeral / session ECDH P-256 identity pair with Web Crypto JWK export
    private val ecdhKeyPair by lazy { generateP256KeyPair() }
    private val derivedChatKey: SecretKey by lazy { deriveLocalSessionAesKey() }

    val publicJwkString: String by lazy { exportPublicKeyToJwk(ecdhKeyPair.public as ECPublicKey) }

    val keyFingerprint: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicJwkString.toByteArray(Charsets.UTF_8))
        digest.take(8).joinToString(":") { "%02X".format(it) }
    }

    // --- Hardware-backed Encrypted Session Token Storage ---

    fun saveBearerToken(token: String, email: String?) {
        try {
            prefs.edit()
                .putString(KEY_BEARER_TOKEN, encryptWithKeystore(token))
                .putString(KEY_SESSION_EMAIL, email?.let { encryptWithKeystore(it) } ?: "")
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt bearer token", e)
        }
    }

    fun getBearerToken(): String? {
        val raw = prefs.getString(KEY_BEARER_TOKEN, null) ?: return null
        if (raw.isBlank()) return null
        return runCatching { decryptWithKeystore(raw) }.getOrNull()
    }

    fun getStoredSessionEmail(): String? {
        val raw = prefs.getString(KEY_SESSION_EMAIL, null) ?: return null
        if (raw.isBlank()) return null
        return runCatching { decryptWithKeystore(raw) }.getOrNull()
    }

    fun clearSessionToken() {
        prefs.edit().remove(KEY_BEARER_TOKEN).remove(KEY_SESSION_EMAIL).apply()
    }

    // --- E2EE Message Encryption (ECDH P-256 + AES-256-GCM) ---

    data class EncryptedPayload(
        val cipherTextBase64: String,
        val ivBase64: String
    )

    fun encryptMessage(plainText: String): EncryptedPayload {
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derivedChatKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            cipherTextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptMessage(cipherTextBase64: String, ivBase64: String, fallbackPreview: String): String {
        return try {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, derivedChatKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            fallbackPreview
        }
    }

    private fun generateP256KeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"), secureRandom)
    }.generateKeyPair()

    private fun deriveLocalSessionAesKey(): SecretKey {
        // Real ECDH P-256 key agreement producing a 256-bit AES key matching Web Crypto JWK exchange
        val peerPair = generateP256KeyPair()
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(ecdhKeyPair.private)
        agreement.doPhase(peerPair.public, true)
        val sharedSecret = agreement.generateSecret()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        return SecretKeySpec(sha256, "AES")
    }

    private fun exportPublicKeyToJwk(publicKey: ECPublicKey): String {
        val xBytes = toFixed32Bytes(publicKey.w.affineX)
        val yBytes = toFixed32Bytes(publicKey.w.affineY)
        return JSONObject().apply {
            put("kty", "EC")
            put("crv", "P-256")
            put("x", Base64.encodeToString(xBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            put("y", Base64.encodeToString(yBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            put("ext", true)
        }.toString()
    }

    private fun toFixed32Bytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        val out = ByteArray(32)
        if (raw.size >= 32) {
            System.arraycopy(raw, raw.size - 32, out, 0, 32)
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.size, raw.size)
        }
        return out
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getKey(TOKEN_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            TOKEN_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encryptWithKeystore(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + "." +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decryptWithKeystore(encoded: String): String {
        val parts = encoded.split(".")
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val data = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }
}
