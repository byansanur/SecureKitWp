package com.byan.securekit.core

/**
 * Enum lingkungan eksekusi aplikasi untuk menyesuaikan perilaku logging dan toleransi keamanan.
 */
enum class SecurityEnvironment {
    DEV,
    QA,
    PT,       // Penetration Testing / Performance Testing
    STAGING,
    PROD;

    /**
     * Memeriksa apakah lingkungan ini termasuk lingkungan non-produksi (Development / Testing).
     */
    val isNonProd: Boolean
        get() = this != PROD
}
