package com.byan.securekit.database

import android.content.Context
import com.byan.securekit.core.SecureResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class DatabaseIntegrityCheckerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dbDir: File
    private lateinit var mockContext: Context
    private val integrityChecker = DatabaseIntegrityChecker()

    @Before
    fun setUp() {
        dbDir = tempFolder.newFolder("databases")
        mockContext = mock(Context::class.java)
        `when`(mockContext.getDatabasePath("test_plain.db")).thenReturn(File(dbDir, "test_plain.db"))
        `when`(mockContext.getDatabasePath("test_encrypted.db")).thenReturn(File(dbDir, "test_encrypted.db"))
        `when`(mockContext.getDatabasePath("non_existent.db")).thenReturn(File(dbDir, "non_existent.db"))
    }

    @Test
    fun `test plaintext sqlite database header returns true`() {
        val dbFile = File(dbDir, "test_plain.db")
        dbFile.writeBytes("SQLite format 3\u0000SomePayloadBytesHere...".toByteArray(Charsets.US_ASCII))

        val result = integrityChecker.isPlaintextDatabase(mockContext, "test_plain.db")

        assertTrue(result is SecureResult.Success)
        assertTrue((result as SecureResult.Success).data)
    }

    @Test
    fun `test encrypted database header returns false`() {
        val dbFile = File(dbDir, "test_encrypted.db")
        // Write random encrypted bytes (not SQLite format 3 magic header)
        dbFile.writeBytes(byteArrayOf(0x00, 0x1F, 0x2E, 0x3D, 0x4C, 0x5B, 0x6A, 0x79, 0x88.toByte(), 0x97.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))

        val result = integrityChecker.isPlaintextDatabase(mockContext, "test_encrypted.db")

        assertTrue(result is SecureResult.Success)
        assertFalse((result as SecureResult.Success).data)
    }

    @Test
    fun `test non existent database returns false`() {
        val result = integrityChecker.isPlaintextDatabase(mockContext, "non_existent.db")

        assertTrue(result is SecureResult.Success)
        assertFalse((result as SecureResult.Success).data)
    }
}
