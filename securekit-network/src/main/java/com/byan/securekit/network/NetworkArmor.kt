package com.byan.securekit.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.byan.securekit.core.SecurityLogger
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Modul Armor Jaringan untuk Certificate Pinning dan Deteksi Anomali Proxy/VPN.
 */
class NetworkArmor(
    private val logger: SecurityLogger = SecurityLogger.Silent
) {

    /**
     * Membuat [OkHttpClient] dengan SSL Certificate Pinning.
     */
    fun createSecureHttpClient(domainName: String, certPins: List<String>): OkHttpClient {
        val certificatePinnerBuilder = CertificatePinner.Builder()
        for (pin in certPins) {
            certificatePinnerBuilder.add(domainName, pin)
        }

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinnerBuilder.build())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Memeriksa apakah jaringan dianggap aman (tidak ada Proxy HTTP aktif dan tidak ada VPN).
     */
    fun isNetworkSecure(context: Context): Boolean {
        val proxy = isProxyActive(context)
        val vpn = isVpnActive(context)
        logger.d("NetworkArmor", "Proxy active: $proxy, VPN active: $vpn")
        return !proxy && !vpn
    }

    /**
     * Deteksi proxy melalui System Property dan ProxySelector default.
     */
    fun isProxyActive(context: Context): Boolean {
        val httpProxyHost = System.getProperty("http.proxyHost")
        val httpProxyPort = System.getProperty("http.proxyPort")
        val httpsProxyHost = System.getProperty("https.proxyHost")
        val httpsProxyPort = System.getProperty("https.proxyPort")

        if (!httpProxyHost.isNullOrEmpty() && !httpProxyPort.isNullOrEmpty()) return true
        if (!httpsProxyHost.isNullOrEmpty() && !httpsProxyPort.isNullOrEmpty()) return true

        return try {
            val defaultProxySelector = ProxySelector.getDefault()
            val proxies = defaultProxySelector?.select(URI.create("https://www.google.com"))
            proxies?.any { it.type() == Proxy.Type.HTTP } ?: false
        } catch (e: Exception) {
            logger.e("NetworkArmor", "Error checking ProxySelector", e)
            false
        }
    }

    /**
     * Deteksi koneksi VPN aktif melalui [ConnectivityManager].
     */
    fun isVpnActive(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) {
            logger.e("NetworkArmor", "Error checking VPN active status", e)
            false
        }
    }
}
