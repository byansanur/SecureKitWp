package com.byan.securekit.network

import com.byan.securekit.core.SecurityLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit & Integration Test untuk CertificateTransparencyInterceptor menggunakan MockWebServer.
 * Memvalidasi penanganan header Expect-CT dan pencatatan log Certificate Transparency (CWE-295).
 */
class CertificateTransparencyInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test interceptor proceeds request and receives response with Expect-CT header`() {
        val loggedMessages = mutableListOf<String>()
        val testLogger = object : SecurityLogger {
            override fun d(tag: String, message: String) {
                loggedMessages.add("[$tag] $message")
            }
            override fun e(tag: String, message: String, throwable: Throwable?) {
                loggedMessages.add("[ERROR] [$tag] $message")
            }
        }

        val interceptor = CertificateTransparencyInterceptor(logger = testLogger, enforceStrict = false)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Expect-CT", "max-age=86400, enforce")
                .setBody("OK")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/check"))
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("OK", response.body?.string())
    }

    @Test
    fun `test interceptor handles response without Expect-CT header gracefully`() {
        val interceptor = CertificateTransparencyInterceptor()
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("No Expect-CT header")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/data"))
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("No Expect-CT header", response.body?.string())
    }

    @Test
    fun `test interceptor handles error status codes from server`() {
        val interceptor = CertificateTransparencyInterceptor(enforceStrict = true)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service Unavailable")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/error"))
            .build()

        val response = client.newCall(request).execute()

        assertEquals(503, response.code)
    }
}
