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

class PathValidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sandboxDir: File
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        sandboxDir = tempFolder.newFolder("sandbox")
        mockContext = mock(Context::class.java)
        `when`(mockContext.filesDir).thenReturn(sandboxDir)
    }

    @Test
    fun `test valid file name inside sandbox`() {
        val result = PathValidation.getValidatedFile(
            context = mockContext,
            fileName = "secrets.bin",
            baseDir = sandboxDir
        )
        assertEquals(File(sandboxDir, "secrets.bin").canonicalPath, result.canonicalPath)
    }

    @Test
    fun `test valid nested file name inside sandbox`() {
        val subDir = File(sandboxDir, "subfolder")
        subDir.mkdirs()

        val result = PathValidation.getValidatedFile(
            context = mockContext,
            fileName = "subfolder/notes.bin",
            baseDir = sandboxDir
        )
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test(expected = SecurityException::class)
    fun `test path traversal parent directory attempt throws SecurityException`() {
        PathValidation.getValidatedFile(
            context = mockContext,
            fileName = "../outside.txt",
            baseDir = sandboxDir
        )
    }

    @Test(expected = SecurityException::class)
    fun `test path traversal complex dot attempt throws SecurityException`() {
        PathValidation.getValidatedFile(
            context = mockContext,
            fileName = "subfolder/../../malicious.txt",
            baseDir = sandboxDir
        )
    }
}
