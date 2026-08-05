package com.byan.securekit.database

import android.content.Context
import com.byan.securekit.core.PathValidation
import com.byan.securekit.core.SecureResult
import com.byan.securekit.core.SecurityLogger
import java.io.FileInputStream

/**
 * Pemeriksa integritas header database SQLite untuk mengidentifikasi apakah database bersifat plaintext (unencrypted) atau encrypted.
 */
class DatabaseIntegrityChecker(
    private val logger: SecurityLogger = SecurityLogger.Silent
) {

    companion object {
        // Standard SQLite magic header bytes: "SQLite format 3\0"
        private val SQLITE_HEADER_BYTES = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }

    /**
     * Memeriksa apakah file database pada `dbName` merupakan file SQLite plaintext (tidak terenkripsi).
     * Returns [SecureResult.Success] containing `true` jika file berupa plaintext, `false` jika terenkripsi/encrypted.
     */
    fun isPlaintextDatabase(context: Context, dbName: String): SecureResult<Boolean> {
        return try {
            val baseDirectory = context.getDatabasePath(dbName).parentFile ?: context.filesDir
            val dbFile = PathValidation.getValidatedFile(context, dbName, baseDir = baseDirectory)
            if (!dbFile.exists()) {
                return SecureResult.Success(false)
            }

            val header = ByteArray(16)
            FileInputStream(dbFile).use { fis ->
                val bytesRead = fis.read(header)
                if (bytesRead < 16) {
                    return SecureResult.Success(false)
                }
            }

            val isPlaintext = header.contentEquals(SQLITE_HEADER_BYTES)
            if (isPlaintext) {
                logger.e("DatabaseIntegrityChecker", "Peringatan: File database '$dbName' terdeteksi PLAINTEXT (tidak terenkripsi)!")
            }

            SecureResult.Success(isPlaintext)
        } catch (e: Exception) {
            logger.e("DatabaseIntegrityChecker", "Gagal memeriksa header database '$dbName'", e)
            SecureResult.Error(e)
        }
    }
}
