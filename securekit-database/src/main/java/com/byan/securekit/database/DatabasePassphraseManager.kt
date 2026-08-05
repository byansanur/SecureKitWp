package com.byan.securekit.database

import android.content.Context
import com.byan.securekit.core.SecureResult
import com.byan.securekit.core.SecurityLogger
import com.byan.securekit.crypto.CryptoConfig
import com.byan.securekit.crypto.SecureVault
import java.security.SecureRandom
import java.util.Arrays

/**
 * Pengelola kata sandi (passphrase 256-bit) terenkripsi untuk database SQLCipher / Room.
 * Menghasilkan kunci acak yang aman dan menyimpannya di Android Keystore / Tink AEAD.
 */
class DatabasePassphraseManager(
    private val config: CryptoConfig = CryptoConfig(prefsName = "SecureKitDatabasePrefs"),
    private val logger: SecurityLogger = SecurityLogger.Silent
) {

    private val vault = SecureVault(config = config, logger = logger)

    /**
     * Mengambil atau membuat passphrase 256-bit yang terenkripsi Keystore.
     */
    fun getOrCreatePassphrase(context: Context, keyAlias: String = "DB_PASSPHRASE"): SecureResult<ByteArray> {
        return try {
            val existing = vault.getString(context, keyAlias)
            if (existing is SecureResult.Success && !existing.data.isNullOrEmpty()) {
                val hexString = existing.data!!
                return SecureResult.Success(hexToBytes(hexString))
            }

            // Generate new 32-byte (256-bit) cryptographically strong random passphrase
            val newPassphraseBytes = ByteArray(32)
            SecureRandom().nextBytes(newPassphraseBytes)

            val hexPassphrase = bytesToHex(newPassphraseBytes)
            val saveResult = vault.saveString(context, keyAlias, hexPassphrase)

            if (saveResult is SecureResult.Error) {
                return SecureResult.Error(saveResult.cause, "Gagal menyimpan passphrase database ke Tink Vault")
            }

            SecureResult.Success(newPassphraseBytes)
        } catch (e: Exception) {
            logger.e("DatabasePassphraseManager", "Gagal mengelola passphrase database", e)
            SecureResult.Error(e)
        }
    }

    /**
     * Membersihkan byte array passphrase dari memori (zero-filling).
     */
    fun wipePassphrase(passphrase: ByteArray) {
        Arrays.fill(passphrase, 0.toByte())
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
