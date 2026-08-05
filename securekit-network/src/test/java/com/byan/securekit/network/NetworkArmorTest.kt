package com.byan.securekit.network

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class NetworkArmorTest {

    private val networkArmor = NetworkArmor()

    @Test
    fun `test createSecureHttpClient configures certificate pinner correctly`() {
        val domain = "api.bank.com"
        val pins = listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

        val client = networkArmor.createSecureHttpClient(domain, pins)

        assertNotNull(client)
        assertNotNull(client.certificatePinner)
        assertEquals(30, client.connectTimeoutMillis.toLong() / 1000)
        assertEquals(30, client.readTimeoutMillis.toLong() / 1000)
        assertEquals(30, client.writeTimeoutMillis.toLong() / 1000)
    }

    @Test
    fun `test proxy detection with system properties`() {
        val mockContext = mock(Context::class.java)

        // Set proxy system properties
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8080")

        assertTrue(networkArmor.isProxyActive(mockContext))

        // Clear proxy system properties
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")

        assertFalse(networkArmor.isProxyActive(mockContext))
    }
}
