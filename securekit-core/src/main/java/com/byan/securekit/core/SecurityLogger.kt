package com.byan.securekit.core

/**
 * Interface pencatatan log internal yang dapat dikustomisasi oleh aplikasi konsumen.
 * Secara default, library tidak akan mencetak log ke Logcat publik demi alasan keamanan.
 */
interface SecurityLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)

    object Silent : SecurityLogger {
        override fun d(tag: String, message: String) {}
        override fun e(tag: String, message: String, throwable: Throwable?) {}
    }
}
