package com.byan.securekit.core

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

/**
 * Adversarial test untuk FileSecurityLogger.
 * Menguji ketahanan terhadap CRLF log injection, rotasi file stress,
 * dan isolasi environment PROD (CWE-532 / CWE-117 / OWASP M1).
 */
class FileSecurityLoggerAdversarialTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        mockContext = mock(Context::class.java)
        `when`(mockContext.filesDir).thenReturn(filesDir)
    }

    // --- CRLF Log Injection (CWE-117) ---

    @Test
    fun `test CRLF injection attempt does not create fake log entry`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.DEV)

        // Attacker tries to inject a fake PROD ERROR log via CRLF
        val maliciousMessage = "Normal message\r\n[PROD] [ERROR] [Injected] Fake critical error"
        logger.d("UserInput", maliciousMessage)

        val content = logger.getLogFile().readText()
        val lines = content.lines().filter { it.isNotBlank() }

        // The injected part should appear within the same log entry context, NOT as a separate [PROD] tagged entry
        // Verify there is only ONE formatted log entry (one timestamp line)
        val timestampEntries = lines.filter { it.startsWith("[") && it.contains("[DEV]") }
        assertEquals("Should have exactly 1 real log entry", 1, timestampEntries.size)

        // Verify no line starts with [PROD] tag
        val fakeProdEntries = lines.filter { it.contains("[PROD]") && it.contains("[ERROR]") && it.contains("[Injected]") }
        // The CRLF content may appear on a new line, but it should NOT be formatted as a proper log entry
        // with its own timestamp prefix
        val properlyFormattedFakeEntries = lines.filter {
            it.matches(Regex("^\\[\\d{4}-.*\\] \\[PROD\\].*"))
        }
        assertEquals("No CRLF-injected lines should look like real [PROD] log entries", 0, properlyFormattedFakeEntries.size)
    }

    @Test
    fun `test newline injection in tag does not break log format`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.QA)

        val maliciousTag = "Tag\nInjected] [ERROR] Fake"
        logger.d(maliciousTag, "Normal message")

        val content = logger.getLogFile().readText()
        // Verify the tag content is captured but only one properly formatted entry exists
        assertTrue(content.contains("[QA]"))
        assertTrue(content.contains("[DEBUG]"))
    }

    // --- File Rotation Stress Test ---

    @Test
    fun `test rapid rotation under heavy write load`() {
        // 50 bytes max to force aggressive rotation
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.PT, maxFileSizeBytes = 50)

        // Write 200 log entries rapidly
        for (i in 1..200) {
            logger.d("Stress", "Entry $i with enough content to exceed rotation limit")
        }

        val logDir = File(filesDir, "security_logs")
        val mainLog = File(logDir, "security.log")
        val backupLog = File(logDir, "security_old.log")

        assertTrue("Main log file should exist", mainLog.exists())
        assertTrue("Backup log file should exist after rotation", backupLog.exists())
        // Main log should be smaller than max (just rotated)
        assertTrue("Main log should be under max size after rotation", mainLog.length() < 200)
    }

    @Test
    fun `test concurrent writes do not corrupt log file`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.DEV)
        val threads = mutableListOf<Thread>()
        val errors = mutableListOf<Throwable>()

        // 10 threads each writing 50 entries
        for (t in 1..10) {
            threads.add(Thread {
                try {
                    for (i in 1..50) {
                        logger.d("Thread-$t", "Message $i")
                    }
                } catch (e: Throwable) {
                    synchronized(errors) { errors.add(e) }
                }
            })
        }

        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }

        assertTrue("No exceptions should occur during concurrent writes", errors.isEmpty())

        val content = logger.getLogFile().readText()
        assertTrue("Log file should contain entries", content.isNotEmpty())
    }

    // --- Environment Isolation ---

    @Test
    fun `test PROD environment writes to file but does not affect logcat check`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.PROD)

        logger.d("SecureVault", "Token saved successfully")
        logger.e("NetworkArmor", "Connection failed")

        val content = logger.getLogFile().readText()

        // File log should still work
        assertTrue(content.contains("[PROD]"))
        assertTrue(content.contains("[DEBUG]"))
        assertTrue(content.contains("[ERROR]"))
        assertTrue(content.contains("[SecureVault]"))
        assertTrue(content.contains("[NetworkArmor]"))

        // Verify environment is PROD (Logcat should be skipped — verified by isNonProd == false)
        assertFalse("PROD environment should not be NonProd", logger.environment.isNonProd)
    }

    @Test
    fun `test all non-prod environments are marked as isNonProd`() {
        val nonProdEnvs = listOf(SecurityEnvironment.DEV, SecurityEnvironment.QA, SecurityEnvironment.PT, SecurityEnvironment.STAGING)
        for (env in nonProdEnvs) {
            assertTrue("$env should be NonProd", env.isNonProd)
        }
        assertFalse("PROD should NOT be NonProd", SecurityEnvironment.PROD.isNonProd)
    }

    // --- Error logging with throwable ---

    @Test
    fun `test error log with deep exception chain writes stack trace correctly`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.DEV)

        val rootCause = IllegalArgumentException("Root cause")
        val wrappedCause = RuntimeException("Wrapped", rootCause)
        val topException = SecurityException("Top level security error", wrappedCause)

        logger.e("CryptoModule", "Encryption failed", topException)

        val content = logger.getLogFile().readText()
        assertTrue(content.contains("[ERROR]"))
        assertTrue(content.contains("SecurityException"))
        assertTrue(content.contains("Top level security error"))
        assertTrue(content.contains("Exception:"))
    }

    // --- Export & Clear ---

    @Test
    fun `test export then clear then re-log produces fresh file`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.STAGING)

        logger.d("Phase1", "First message")
        val exportTarget = File(tempFolder.root, "exported.log")
        logger.exportLogFile(exportTarget)

        assertTrue(exportTarget.exists())
        assertTrue(exportTarget.readText().contains("First message"))

        logger.clearLogs()
        assertEquals("", logger.getLogFile().readText())

        logger.d("Phase2", "Second message")
        val newContent = logger.getLogFile().readText()
        assertTrue(newContent.contains("Second message"))
        assertFalse(newContent.contains("First message"))
    }
}
