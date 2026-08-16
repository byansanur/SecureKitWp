package com.byan.securekit.core

import java.security.SecureRandom
import java.util.Arrays

/**
 * Struktur data terenkripsi memori untuk mengelola string sensitif (PIN, Password, Token).
 *
 * Perlindungan:
 * 1. Tidak menggunakan `String` immutable.
 * 2. Meng-XOR array internal menggunakan random key dari [SecureRandom].
 * 3. Array mentah input segera di-zero out saat pembuatan.
 * 4. Implementasi [AutoCloseable] wajib di-close / [use] untuk zero-out buffer memori.
 */
class SecureCharArray(clearText: CharArray) : AutoCloseable {

    private val lock = Any()
    private var obfuscatedData: ByteArray
    private var key: ByteArray
    private var isCleared = false

    init {
        val random = SecureRandom()
        val length = clearText.size

        obfuscatedData = ByteArray(length * 2)
        key = ByteArray(length * 2)
        random.nextBytes(key)

        for (i in 0 until length) {
            val charValue = clearText[i].code
            val b1 = (charValue shr 8).toByte()
            val b2 = charValue.toByte()

            obfuscatedData[i * 2] = (b1.toInt() xor key[i * 2].toInt()).toByte()
            obfuscatedData[i * 2 + 1] = (b2.toInt() xor key[i * 2 + 1].toInt()).toByte()

            clearText[i] = '\u0000'
        }
    }

    /**
     * Membuka enkripsi XOR sementara dalam lingkup lambda block.
     * Segera membersihkan (zero-out) array temporary setelah lambda selesai.
     */
    fun <R> useClearText(block: (CharArray) -> R): R {
        val tempClearText: CharArray
        synchronized(lock) {
            check(!isCleared) { "SecureCharArray sudah dibersihkan (cleared/closed)!" }

            val length = obfuscatedData.size / 2
            tempClearText = CharArray(length)

            for (i in 0 until length) {
                val b1 = (obfuscatedData[i * 2].toInt() xor key[i * 2].toInt()).toByte()
                val b2 = (obfuscatedData[i * 2 + 1].toInt() xor key[i * 2 + 1].toInt()).toByte()

                val charValue = ((b1.toInt() and 0xFF) shl 8) or (b2.toInt() and 0xFF)
                tempClearText[i] = charValue.toChar()
            }
        }

        return try {
            block(tempClearText)
        } finally {
            Arrays.fill(tempClearText, '\u0000')
        }
    }

    override fun close() {
        synchronized(lock) {
            if (!isCleared) {
                Arrays.fill(obfuscatedData, 0.toByte())
                Arrays.fill(key, 0.toByte())
                isCleared = true
            }
        }
    }

    fun clear() {
        close()
    }
}
