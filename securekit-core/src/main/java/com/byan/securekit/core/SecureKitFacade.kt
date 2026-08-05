package com.byan.securekit.core

import android.content.Context

/**
 * Facade terpadu dan Kotlin DSL untuk menginisialisasi modul-modul SecureKit.
 */
class SecureKitFacade private constructor(
    val context: Context,
    val logger: SecurityLogger
) {

    class Builder(context: Context) {
        private val appContext: Context = context.applicationContext
        private var logger: SecurityLogger = SecurityLogger.Silent

        fun setLogger(logger: SecurityLogger) = apply {
            this.logger = logger
        }

        fun build(): SecureKitFacade {
            return SecureKitFacade(appContext, logger)
        }
    }

    companion object {
        /**
         * Konfigurasi cepat berbasis Kotlin DSL.
         */
        inline fun build(context: Context, block: Builder.() -> Unit): SecureKitFacade {
            return Builder(context).apply(block).build()
        }
    }
}
