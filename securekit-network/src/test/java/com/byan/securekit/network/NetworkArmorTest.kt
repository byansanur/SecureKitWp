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

        // Set HTTP proxy system properties
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8080")

        assertTrue(networkArmor.isProxyActive(mockContext))

        // Clear HTTP proxy system properties
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")

        assertFalse(networkArmor.isProxyActive(mockContext))
    }

    @Test
    fun `test proxy detection with https system properties`() {
        val mockContext = mock(Context::class.java)

        System.setProperty("https.proxyHost", "10.0.0.1")
        System.setProperty("https.proxyPort", "8888")

        assertTrue(networkArmor.isProxyActive(mockContext))

        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")

        assertFalse(networkArmor.isProxyActive(mockContext))
    }

    @Test
    fun `test proxy detection with empty or blank properties returns false`() {
        val mockContext = mock(Context::class.java)

        System.setProperty("http.proxyHost", "")
        System.setProperty("http.proxyPort", "")

        assertFalse(networkArmor.isProxyActive(mockContext))

        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
    }

    @Test
    fun `test createSecureHttpClient adds CertificateTransparencyInterceptor`() {
        val domain = "api.bank.com"
        val pins = listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

        val client = networkArmor.createSecureHttpClient(domain, pins, enableCertificateTransparency = true)

        assertTrue(client.interceptors.any { it is CertificateTransparencyInterceptor })
    }

    @Test
    fun `test createSecureHttpClient can disable CertificateTransparencyInterceptor`() {
        val domain = "api.bank.com"
        val pins = listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

        val client = networkArmor.createSecureHttpClient(domain, pins, enableCertificateTransparency = false)

        assertFalse(client.interceptors.any { it is CertificateTransparencyInterceptor })
    }

    @Test
    fun `test vpn detection returns false when ConnectivityManager is null`() {
        val mockContext = mock(Context::class.java)
        // null connectivity manager
        assertFalse(networkArmor.isVpnActive(mockContext))
    }
}
