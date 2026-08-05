package com.byan.securekit.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.byan.securekit.core.SecureResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureVaultTest {

    private lateinit var context: android.content.Context
    private lateinit var secureVault: SecureVault

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        secureVault = SecureVault(
            config = CryptoConfig(
                prefsName = "TestSecurePrefs",
                masterKeyUri = "android-keystore://test_master_key_v1"
            )
        )
    }

    @Test
    fun testSaveAndGetStringRoundTrip() {
        val key = "USER_SESSION_TOKEN"
        val value = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

        val saveResult = secureVault.saveString(context, key, value)
        assertTrue(saveResult is SecureResult.Success)

        val getResult = secureVault.getString(context, key)
        assertTrue(getResult is SecureResult.Success)
        assertEquals(value, (getResult as SecureResult.Success).data)
    }

    @Test
    fun testGetNonExistentStringReturnsNullSuccess() {
        val result = secureVault.getString(context, "NON_EXISTENT_KEY_XYZ")
        assertTrue(result is SecureResult.Success)
        assertNull((result as SecureResult.Success).data)
    }

    @Test
    fun testWriteAndReadFileRoundTrip() {
        val fileName = "encrypted_test_data.bin"
        val testPayload = "Top Secret Financial Records Payload Data 1234567890".toByteArray()

        val writeResult = secureVault.writeFile(context, fileName, testPayload)
        assertTrue(writeResult is SecureResult.Success)

        val readResult = secureVault.readFile(context, fileName)
        assertTrue(readResult is SecureResult.Success)
        assertArrayEquals(testPayload, (readResult as SecureResult.Success).data)
    }

    @Test
    fun testPathTraversalInWriteFileReturnsError() {
        val maliciousFileName = "../malicious_file.bin"
        val payload = "Hacked".toByteArray()

        val writeResult = secureVault.writeFile(context, maliciousFileName, payload)
        assertTrue(writeResult is SecureResult.Error)
        assertTrue((writeResult as SecureResult.Error).cause is SecurityException)
    }

    @Test
    fun testPathTraversalInReadFileReturnsError() {
        val maliciousFileName = "../malicious_file.bin"

        val readResult = secureVault.readFile(context, maliciousFileName)
        assertTrue(readResult is SecureResult.Error)
        assertTrue((readResult as SecureResult.Error).cause is SecurityException)
    }
}
