package com.byan.securekit.crypto

import android.content.Context
import com.byan.securekit.core.PathValidation
import com.byan.securekit.core.SecureResult
import com.byan.securekit.core.SecurityLogger
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import com.google.crypto.tink.subtle.Base64
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Modul Brankas Aman untuk enkripsi data di SharedPreferences dan File I/O
 * menggunakan Google Tink (AES-256 GCM & Streaming AEAD HKDF).
 */
class SecureVault(
    private val config: CryptoConfig = CryptoConfig(),
    private val logger: SecurityLogger = SecurityLogger.Silent
) {

    companion object {
        @Volatile
        private var isTinkRegistered = false

        private fun ensureTinkRegistered() {
            if (!isTinkRegistered) {
                synchronized(this) {
                    if (!isTinkRegistered) {
                        AeadConfig.register()
                        StreamingAeadConfig.register()
                        isTinkRegistered = true
                    }
                }
            }
        }
    }

    init {
        ensureTinkRegistered()
    }

    private fun getTinkAead(context: Context): Aead {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, config.aeadKeysetPrefName, config.prefsName)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(config.masterKeyUri)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun getTinkStreamingAead(context: Context): StreamingAead {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, config.streamingKeysetPrefName, config.prefsName)
            .withKeyTemplate(AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate())
            .withMasterKeyUri(config.streamingMasterKeyUri)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }

    /**
     * Menyimpan string sensitif ke SharedPreferences setelah dienkripsi dengan Tink AEAD.
     */
    fun saveString(context: Context, key: String, value: String): SecureResult<Unit> {
        return try {
            val aead = getTinkAead(context)
            val ciphertext = aead.encrypt(value.toByteArray(), key.toByteArray())
            val encodedValue = Base64.encodeToString(ciphertext, Base64.DEFAULT)

            val prefs = context.getSharedPreferences(config.prefsName, Context.MODE_PRIVATE)
            prefs.edit().putString(key, encodedValue).apply()
            SecureResult.Success(Unit)
        } catch (e: Exception) {
            logger.e("SecureVault", "Gagal menyimpan string terenkripsi", e)
            SecureResult.Error(e)
        }
    }

    /**
     * Mengambil dan mendekripsi string dari SharedPreferences.
     */
    fun getString(context: Context, key: String): SecureResult<String?> {
        return try {
            val prefs = context.getSharedPreferences(config.prefsName, Context.MODE_PRIVATE)
            val encodedValue = prefs.getString(key, null) ?: return SecureResult.Success(null)

            val aead = getTinkAead(context)
            val ciphertext = Base64.decode(encodedValue, Base64.DEFAULT)
            val decrypted = aead.decrypt(ciphertext, key.toByteArray())
            SecureResult.Success(String(decrypted))
        } catch (e: Exception) {
            logger.e("SecureVault", "Gagal membaca/mendekripsi string", e)
            SecureResult.Error(e)
        }
    }

    /**
     * Menulis file terenkripsi secara efisien (streaming) menggunakan Tink Streaming AEAD.
     * Dilengkapi proteksi Path Traversal otomatis.
     */
    fun writeFile(context: Context, fileName: String, content: ByteArray): SecureResult<Unit> {
        return try {
            val targetFile = PathValidation.getValidatedFile(context, fileName)
            val streamingAead = getTinkStreamingAead(context)

            FileOutputStream(targetFile).use { fos ->
                streamingAead.newEncryptingStream(fos, fileName.toByteArray()).use { encryptingStream ->
                    encryptingStream.write(content)
                }
            }
            SecureResult.Success(Unit)
        } catch (e: Exception) {
            logger.e("SecureVault", "Gagal menulis file terenkripsi", e)
            SecureResult.Error(e)
        }
    }

    /**
     * Membaca dan mendekripsi file dari direktori aplikasi.
     */
    fun readFile(context: Context, fileName: String): SecureResult<ByteArray?> {
        return try {
            val targetFile = PathValidation.getValidatedFile(context, fileName)
            if (!targetFile.exists()) {
                return SecureResult.Success(null)
            }

            val streamingAead = getTinkStreamingAead(context)
            val baos = ByteArrayOutputStream()

            FileInputStream(targetFile).use { fis ->
                streamingAead.newDecryptingStream(fis, fileName.toByteArray()).use { decryptingStream ->
                    decryptingStream.copyTo(baos)
                }
            }
            SecureResult.Success(baos.toByteArray())
        } catch (e: Exception) {
            logger.e("SecureVault", "Gagal membaca file terenkripsi", e)
            SecureResult.Error(e)
        }
    }
}
