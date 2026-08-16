package com.byan.securekitwp

import android.content.Context
import com.byan.securekit.core.PathValidation
import com.byan.securekit.core.SecureCharArray
import com.byan.securekit.core.SecureResult
import com.byan.securekit.database.DatabaseIntegrityChecker
import com.byan.securekit.integrity.IntegrityChecker
import com.byan.securekit.network.CertificateTransparencyInterceptor
import com.byan.securekit.network.NetworkArmor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

/**
 * End-to-End Automated Security Penetration Test Suite (OWASP MASVS & OWASP Mobile Top 10).
 * Mensimulasikan vektor serangan nyata terhadap integrasi library SecureKit pada aplikasi.
 */
class AppPenetrationTestSuite {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sandboxDir: File
    private lateinit var mockContext: Context

    private val integrityChecker = IntegrityChecker()
    private val networkArmor = NetworkArmor()
    private val dbIntegrityChecker = DatabaseIntegrityChecker()

    @Before
    fun setUp() {
        sandboxDir = tempFolder.newFolder("app_sandbox")
        mockContext = mock(Context::class.java)
        `when`(mockContext.filesDir).thenReturn(sandboxDir)
        `when`(mockContext.getDatabasePath("unencrypted.db")).thenReturn(File(sandboxDir, "unencrypted.db"))
        `when`(mockContext.getDatabasePath("secure.db")).thenReturn(File(sandboxDir, "secure.db"))
    }

    // ======================================================================
    // 🛡️ Attack Vector 1: Memory Dumping & PIN In-Memory Extraction
    // MASVS-STORAGE-1 / CWE-316
    // ======================================================================

    @Test
    fun `pentest vector 1 - sensitive PIN is zeroed in memory after processing`() {
        val rawPin = charArrayOf('9', '8', '7', '6')
        val securePin = SecureCharArray(rawPin)

        // 1. Verify original input array was wiped immediately at construction
        for (char in rawPin) {
            assertEquals("Raw input char must be zeroed immediately", '\u0000', char)
        }

        // 2. Process PIN in safe closure
        var capturedRef: CharArray? = null
        val verificationSuccess = securePin.useClearText { clearChars ->
            capturedRef = clearChars
            String(clearChars) == "9876"
        }
        assertTrue(verificationSuccess)

        // 3. Verify in-memory buffer was zeroed immediately after closure
        capturedRef?.let { temp ->
            for (i in temp.indices) {
                assertEquals("Temporary cleartext char at $i must be zeroed", '\u0000', temp[i])
            }
        }

        // 4. Verify close() permanently wipes obfuscated internal buffers
        securePin.close()
        try {
            securePin.useClearText { }
            org.junit.Assert.fail("Accessing closed SecureCharArray must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    // ======================================================================
    // 🛡️ Attack Vector 2: Sandbox Escape & Directory Traversal
    // MASVS-CODE-2 / CWE-22
    // ======================================================================

    @Test
    fun `pentest vector 2 - directory traversal attacks are blocked by PathValidation`() {
        val maliciousPayloads = listOf(
            "../etc/passwd",
            "../../data/system/users/0/settings.xml",
            "/etc/shadow",
            "/data/data/com.other.app/databases/app.db",
            "subfolder/../../../escaped_file.bin"
        )

        for (payload in maliciousPayloads) {
            var blocked = false
            try {
                PathValidation.getValidatedFile(mockContext, payload, sandboxDir)
            } catch (e: SecurityException) {
                blocked = true
            }
            assertTrue("Traversal payload '$payload' MUST be blocked by PathValidation", blocked)
        }
    }

    // ======================================================================
    // 🛡️ Attack Vector 3: Man-in-the-Middle (MitM) & Proxy Anomaly Detection
    // MASVS-NETWORK-1 / CWE-295
    // ======================================================================

    @Test
    fun `pentest vector 3 - active HTTP proxy anomaly is detected and network deemed insecure`() {
        // Attacker activates Burp Suite / OWASP ZAP proxy
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8080")

        try {
            assertTrue("Proxy detection must be active", networkArmor.isProxyActive(mockContext))
            assertFalse("Network must be marked INSECURE when proxy is active", networkArmor.isNetworkSecure(mockContext))
        } finally {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
        }

        // Clear proxy -> Network secure again
        assertTrue("Network should be secure when no proxy/VPN is active", networkArmor.isNetworkSecure(mockContext))
    }

    @Test
    fun `pentest vector 3 - secure http client is configured with Certificate Pinning and CT Interceptor`() {
        val client = networkArmor.createSecureHttpClient(
            domainName = "api.bank.com",
            certPins = listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
            enableCertificateTransparency = true
        )

        assertNotNull("CertificatePinner must be configured", client.certificatePinner)
        assertTrue(
            "CertificateTransparencyInterceptor must be present in interceptor chain",
            client.interceptors.any { it is CertificateTransparencyInterceptor }
        )
    }

    // ======================================================================
    // 🛡️ Attack Vector 4: Plaintext SQLite Replacement / Database Tampering
    // MASVS-STORAGE-1 / CWE-311
    // ======================================================================

    @Test
    fun `pentest vector 4 - unencrypted plaintext database injection is detected`() {
        // Attacker replaces SQLCipher database with unencrypted SQLite file
        val unencryptedDb = File(sandboxDir, "unencrypted.db")
        unencryptedDb.writeBytes("SQLite format 3\u0000[Attacker Injected Plaintext Table Data]".toByteArray(Charsets.US_ASCII))

        val checkResult = dbIntegrityChecker.isPlaintextDatabase(mockContext, "unencrypted.db")

        assertTrue(checkResult is SecureResult.Success)
        assertTrue("Unencrypted database MUST be flagged as PLAINTEXT (isPlaintext = true)", (checkResult as SecureResult.Success).data)
    }

    @Test
    fun `pentest vector 4 - SQLCipher encrypted database is validated as secure`() {
        // Encrypted database has random ciphertext header (not 'SQLite format 3')
        val secureDb = File(sandboxDir, "secure.db")
        secureDb.writeBytes(byteArrayOf(0x53, 0x51, 0x4C, 0x00, 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()))

        val checkResult = dbIntegrityChecker.isPlaintextDatabase(mockContext, "secure.db")

        assertTrue(checkResult is SecureResult.Success)
        assertFalse("Encrypted database must NOT be flagged as plaintext (isPlaintext = false)", (checkResult as SecureResult.Success).data)
    }

    // ======================================================================
    // 🛡️ Attack Vector 5: Hostile Environment & Emulator Detection
    // MASVS-RESILIENCE-1 / CWE-353
    // ======================================================================

    @Test
    fun `pentest vector 5 - QEMU emulator artifact is detected by IntegrityChecker`() {
        System.setProperty("ro.kernel.qemu", "1")
        try {
            assertTrue("QEMU emulator property must trigger emulator detection", integrityChecker.isEmulator())
        } finally {
            System.clearProperty("ro.kernel.qemu")
        }
    }
}
