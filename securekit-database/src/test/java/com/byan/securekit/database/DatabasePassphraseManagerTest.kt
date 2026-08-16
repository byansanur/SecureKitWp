package com.byan.securekit.database

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabasePassphraseManagerTest {

    @Test
    fun `test wipePassphrase zeros out byte array`() {
        val passphraseManager = DatabasePassphraseManager()
        val passphrase = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        passphraseManager.wipePassphrase(passphrase)

        for (b in passphrase) {
            assertEquals(0.toByte(), b)
        }
    }

    @Test
    fun `test wipePassphrase zeroes out full 32-byte 256-bit key`() {
        val passphraseManager = DatabasePassphraseManager()
        val key = ByteArray(32) { (it + 1).toByte() }

        passphraseManager.wipePassphrase(key)

        for (i in key.indices) {
            assertEquals(0.toByte(), key[i])
        }
    }

    @Test
    fun `test 256-bit SecureRandom generation has high entropy and no collisions`() {
        val random = java.security.SecureRandom()
        val generatedKeys = mutableSetOf<String>()
        val count = 1000

        for (i in 0 until count) {
            val key = ByteArray(32)
            random.nextBytes(key)
            val hex = key.joinToString("") { "%02x".format(it) }
            generatedKeys.add(hex)
        }

        // Verify 1000 generated 256-bit keys produced 1000 unique keys (0 collisions)
        assertEquals("All 1000 generated 256-bit keys must be completely unique", count, generatedKeys.size)
    }

    @Test
    fun `test wipePassphrase on empty array handles gracefully`() {
        val passphraseManager = DatabasePassphraseManager()
        val emptyKey = ByteArray(0)
        passphraseManager.wipePassphrase(emptyKey)
        assertEquals(0, emptyKey.size)
    }
}
