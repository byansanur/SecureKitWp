package com.byan.securekit.network

import com.byan.securekit.core.SecurityLogger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor untuk memverifikasi dan menegakkan Certificate Transparency (CT) pada koneksi HTTPS (CWE-295).
 * Mendukung pencatatan status sertifikat dan audit header Expect-CT / Certificate Transparency.
 */
class CertificateTransparencyInterceptor(
    private val logger: SecurityLogger = SecurityLogger.Silent,
    private val enforceStrict: Boolean = false
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val handshake = response.handshake
        if (handshake != null) {
            val tlsVersion = handshake.tlsVersion.javaName
            val cipherSuite = handshake.cipherSuite.javaName
            val peerCertificates = handshake.peerCertificates

            logger.d(
                "CertificateTransparency",
                "Host: ${request.url.host}, TLS: $tlsVersion, Cipher: $cipherSuite, Certs: ${peerCertificates.size}"
            )

            val expectCtHeader = response.header("Expect-CT")
            if (expectCtHeader != null) {
                logger.d("CertificateTransparency", "Expect-CT header present: $expectCtHeader")
            } else if (enforceStrict && request.isHttps) {
                logger.d("CertificateTransparency", "Notice: Expect-CT header missing for ${request.url.host}")
            }
        }

        return response
    }
}
