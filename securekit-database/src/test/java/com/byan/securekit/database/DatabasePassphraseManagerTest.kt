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
}
