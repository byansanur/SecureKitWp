package com.byan.securekit.core

/**
 * Representasi hasil operasi keamanan terstruktur (Type-Safe / Sealed).
 * Mencegah pengembalian nilai null ambigu atau pengecualian mentah yang tidak tertangani.
 */
sealed class SecureResult<out T> {
    data class Success<out T>(val data: T) : SecureResult<T>()
    data class Error(val cause: Throwable, val message: String? = cause.message) : SecureResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }
}
