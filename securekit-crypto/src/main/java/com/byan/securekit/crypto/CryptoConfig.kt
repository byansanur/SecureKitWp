package com.byan.securekit.crypto

/**
 * Konfigurasi fleksibel untuk penyimpanan terenkripsi.
 * Memungkinkan aplikasi konsumen menentukan sendiri nama SharedPreferences dan URI Master Key
 * tanpa adanya nilai yang di-hardcode di dalam library.
 */
data class CryptoConfig(
    val prefsName: String = "AppSecurePrefs",
    val aeadKeysetPrefName: String = "secure_aead_keyset",
    val masterKeyUri: String = "android-keystore://tink_master_key_v1",
    val streamingKeysetPrefName: String = "secure_streaming_keyset",
    val streamingMasterKeyUri: String = "android-keystore://tink_streaming_master_key_v1"
)
