package com.byan.securekitwp

import com.byan.securekit.core.SecureCharArray
import com.byan.securekit.core.SecureResult
import com.byan.securekit.crypto.CryptoConfig
import com.byan.securekit.integrity.IntegrityChecker
import com.byan.securekit.network.NetworkArmor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSecurityIntegrationTest {

    private val integrityChecker = IntegrityChecker()
    private val networkArmor = NetworkArmor()
    private val cryptoConfig = CryptoConfig(prefsName = "CustomAppPrefs")

    @Test
    fun testIntegrityCheckerInitializationInAppModule() {
        assertNotNull(integrityChecker)
        // Verify isEmulator heuristic evaluates without NPE
        val isEmu = integrityChecker.isEmulator()
        assertNotNull(isEmu)
    }

    @Test
    fun testNetworkArmorHttpClientBuilderInAppModule() {
        val domain = "api.bank-secure.com"
        val pins = listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

        val okHttpClient = networkArmor.createSecureHttpClient(domain, pins)
        assertNotNull(okHttpClient)
        assertNotNull(okHttpClient.certificatePinner)
    }

    @Test
    fun testCryptoConfigCustomNamesInAppModule() {
        assertEquals("CustomAppPrefs", cryptoConfig.prefsName)
        assertEquals("secure_aead_keyset", cryptoConfig.aeadKeysetPrefName)
    }

    @Test
    fun testSecureCharArrayWipingInAppModule() {
        val pin = charArrayOf('1', '2', '3', '4')
        val securePin = SecureCharArray(pin)

        // Verify input array was zeroed immediately
        for (char in pin) {
            assertEquals('\u0000', char)
        }

        securePin.useClearText { clearChars ->
            assertEquals("1234", String(clearChars))
        }

        securePin.close()
    }
}
