package com.byan.securekitwp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.byan.securekit.core.SecureResult
import com.byan.securekit.database.DatabasePassphraseManager
import com.byan.securekit.database.SecureDatabaseFactory

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            // Retrieve or generate secure passphrase via Android Keystore
            val passphraseManager = DatabasePassphraseManager()
            val passphraseResult = passphraseManager.getOrCreatePassphrase(context)
            
            val factoryResult = if (passphraseResult is SecureResult.Success) {
                SecureDatabaseFactory.createSupportFactory(passphraseResult.data)
            } else {
                throw IllegalStateException("Failed to get database passphrase")
            }
            
            val factory = if (factoryResult is SecureResult.Success) {
                factoryResult.data
            } else {
                throw IllegalStateException("Failed to create secure database factory")
            }

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "secure_app_database.db"
            )
            .openHelperFactory(factory)
            .build()
        }
    }
}
