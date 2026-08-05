package com.byan.securekit.core

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class FileSecurityLoggerTest {

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

    @Test
    fun `test logging writes formatted entries with environment tag`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.DEV)
        logger.d("IntegrityChecker", "Root status check completed: false")

        val logFile = logger.getLogFile()
        assertTrue(logFile.exists())

        val content = logFile.readText()
        assertTrue(content.contains("[DEV]"))
        assertTrue(content.contains("[DEBUG]"))
        assertTrue(content.contains("[IntegrityChecker] Root status check completed: false"))
    }

    @Test
    fun `test environment tag for STAGING and QA`() {
        val qaLogger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.QA)
        qaLogger.d("NetworkArmor", "VPN active: false")

        val content = qaLogger.getLogFile().readText()
        assertTrue(content.contains("[QA]"))
        assertTrue(content.contains("[NetworkArmor] VPN active: false"))
    }

    @Test
    fun `test exportLogFile copies log file to target location`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.STAGING)
        logger.d("SecureVault", "Token saved successfully")

        val exportTarget = File(tempFolder.root, "exported_security.log")
        val exportResult = logger.exportLogFile(exportTarget)

        assertTrue(exportResult is SecureResult.Success)
        assertTrue(exportTarget.exists())
        assertTrue(exportTarget.readText().contains("[STAGING]"))
    }

    @Test
    fun `test clearLogs empties the log file`() {
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.PROD)
        logger.d("TestTag", "Log message 1")
        logger.d("TestTag", "Log message 2")

        assertTrue(logger.getLogFile().readText().isNotEmpty())

        logger.clearLogs()

        assertEquals("", logger.getLogFile().readText())
    }

    @Test
    fun `test file rotation when max file size is reached`() {
        // Small max size 100 bytes to trigger rotation easily
        val logger = FileSecurityLogger(mockContext, environment = SecurityEnvironment.PT, maxFileSizeBytes = 100)

        for (i in 1..10) {
            logger.d("StressTag", "Very long log message entry number $i to trigger file rotation automatically")
        }

        val logDir = File(filesDir, "security_logs")
        val backupFile = File(logDir, "security_old.log")
        assertTrue(backupFile.exists())
    }
}
