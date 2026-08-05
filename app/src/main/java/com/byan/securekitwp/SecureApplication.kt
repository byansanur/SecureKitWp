package com.byan.securekitwp

import android.app.Application

class SecureApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Logger with Environment based on BuildConfig
        val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val env = if (isDebug) com.byan.securekit.core.SecurityEnvironment.DEV else com.byan.securekit.core.SecurityEnvironment.PROD
        
        val logger = com.byan.securekit.core.FileSecurityLogger(this, env)
        logger.d("SecureApplication", "Initializing SecureKit in \$env mode")

        // 2. Initialize SecureKit Facade
        com.byan.securekit.core.SecureKitFacade.build(this) {
            setLogger(logger)
        }

        // 3. Initialize Native SecureDatabaseFactory for SQLCipher 4.6.1 + 16KB support
        try {
            com.byan.securekit.database.SecureDatabaseFactory.loadLibs()
            logger.d("SecureApplication", "SecureDatabaseFactory native libs loaded")
        } catch (e: Exception) {
            logger.e("SecureApplication", "Failed to load database libs", e)
        }

        // 4. Activity Lifecycle Protection is handled per activity or via callbacks
        logger.d("SecureApplication", "Secure Application Initialization Complete")
    }
}
