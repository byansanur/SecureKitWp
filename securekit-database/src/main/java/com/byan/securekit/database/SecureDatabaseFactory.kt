package com.byan.securekit.database

import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.byan.securekit.core.SecureResult
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Factory penolong untuk membuat [SupportSQLiteOpenHelper.Factory] berbasis SQLCipher
 * yang kompatibel dengan Room Database (`.openHelperFactory(...)`).
 */
object SecureDatabaseFactory {

    /**
     * Memuat library native SQLCipher. Panggil metode ini sebelum inisialisasi Room database.
     */
    fun loadLibs() {
        System.loadLibrary("sqlcipher")
    }

    /**
     * Membuat [SupportSQLiteOpenHelper.Factory] berbasis SQLCipher menggunakan byte array passphrase.
     */
    fun createSupportFactory(passphrase: ByteArray): SecureResult<SupportSQLiteOpenHelper.Factory> {
        return try {
            val factory = SupportOpenHelperFactory(passphrase)
            SecureResult.Success(factory)
        } catch (e: Exception) {
            SecureResult.Error(e, "Gagal membuat SupportOpenHelperFactory SQLCipher")
        }
    }
}
