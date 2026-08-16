package com.byan.securekit.core

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

/**
 * Adversarial fuzzing test untuk PathValidation.
 * Menguji ketahanan terhadap variasi payload path traversal (CWE-22 / OWASP M4).
 *
 * Catatan:
 * - Backslash (`\`) pada macOS/Linux diperlakukan sebagai karakter literal, bukan separator path.
 *   Pada Android (Linux kernel), backslash juga diperlakukan sebagai literal.
 *   Jadi `..\\escape.txt` TIDAK menjadi traversal pada platform non-Windows.
 * - `File(parent, absolutePath)` pada Java mengabaikan parent jika child adalah path absolut,
 *   tetapi `canonicalPath` akan menyelesaikan ke root filesystem.
 */
class PathValidationAdversarialTest {

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

    // ======================================================================
    // Forward-slash Traversal Payloads — HARUS ditolak
    // ======================================================================

    @Test(expected = SecurityException::class)
    fun `test single parent traversal`() {
        PathValidation.getValidatedFile(mockContext, "../escape.txt", sandboxDir)
    }

    @Test(expected = SecurityException::class)
    fun `test double parent traversal`() {
        PathValidation.getValidatedFile(mockContext, "../../escape.txt", sandboxDir)
    }

    @Test(expected = SecurityException::class)
    fun `test deeply nested escape traversal`() {
        PathValidation.getValidatedFile(mockContext, "a/b/c/d/e/../../../../../../../../etc/passwd", sandboxDir)
    }

    @Test(expected = SecurityException::class)
    fun `test complex nested escape`() {
        PathValidation.getValidatedFile(mockContext, "subfolder/../../malicious.txt", sandboxDir)
    }

    // ======================================================================
    // Backslash Traversal — Pada macOS/Linux/Android, backslash BUKAN separator.
    // `..\\escape.txt` menjadi file bernama literal `..\\escape.txt` di dalam sandbox.
    // Ini BUKAN traversal pada target platform Android (Linux kernel).
    // Test memvalidasi bahwa file tetap di dalam sandbox.
    // ======================================================================

    @Test
    fun `test backslash is treated as literal character on unix platforms`() {
        // On macOS/Linux/Android, backslash is NOT a path separator
        // So "..\\escape.txt" is a literal filename inside sandbox — no traversal
        val result = PathValidation.getValidatedFile(mockContext, "..\\escape.txt", sandboxDir)
        assertTrue(
            "Backslash should be literal on Unix/Android — file stays in sandbox",
            result.canonicalPath.startsWith(sandboxDir.canonicalPath)
        )
    }

    @Test
    fun `test mixed backslash forward slash is literal on unix`() {
        val result = PathValidation.getValidatedFile(mockContext, "..\\..//escape.txt", sandboxDir)
        assertTrue(
            "Mixed backslash should be treated as literal on Unix/Android",
            result.canonicalPath.startsWith(sandboxDir.canonicalPath)
        )
    }

    // ======================================================================
    // Obfuscated Dots — `....//` creates literal `....` directory, not traversal
    // ======================================================================

    @Test
    fun `test extra dots treated as literal directory name`() {
        // `....//escape.txt` creates a path with literal `....` directory — stays in sandbox
        val result = PathValidation.getValidatedFile(mockContext, "....//escape.txt", sandboxDir)
        assertTrue(
            "Extra dots should be literal directory name — file stays in sandbox",
            result.canonicalPath.startsWith(sandboxDir.canonicalPath)
        )
    }

    // ======================================================================
    // Traversal that resolves WITHIN sandbox — should be ALLOWED
    // ======================================================================

    @Test
    fun `test nested traversal that resolves back into sandbox is allowed`() {
        // `a/b/c/../../../escape.txt` resolves to `sandbox/escape.txt` — still inside sandbox
        val result = PathValidation.getValidatedFile(mockContext, "a/b/c/../../../escape.txt", sandboxDir)
        assertTrue(
            "Path that resolves back inside sandbox should be allowed",
            result.canonicalPath.startsWith(sandboxDir.canonicalPath)
        )
    }

    // ======================================================================
    // Absolute Path Injection — HARUS ditolak
    // ======================================================================

    @Test(expected = SecurityException::class)
    fun `test absolute root path unix`() {
        PathValidation.getValidatedFile(mockContext, "/etc/passwd", sandboxDir)
    }

    @Test(expected = SecurityException::class)
    fun `test absolute data path`() {
        PathValidation.getValidatedFile(mockContext, "/data/data/com.other.app/shared_prefs/secrets.xml", sandboxDir)
    }

    @Test(expected = SecurityException::class)
    fun `test absolute proc self maps path`() {
        PathValidation.getValidatedFile(mockContext, "/proc/self/maps", sandboxDir)
    }

    // ======================================================================
    // Null Byte Injection (CWE-158)
    // ======================================================================

    @Test
    fun `test null byte injection throws exception`() {
        var threwException = false
        try {
            PathValidation.getValidatedFile(mockContext, "secret.bin\u0000.txt", sandboxDir)
        } catch (e: Exception) {
            threwException = true
        }
        assertTrue("Null byte in filename should throw an exception", threwException)
    }

    // ======================================================================
    // Valid Paths — HARUS diterima
    // ======================================================================

    @Test
    fun `test simple valid filename`() {
        val result = PathValidation.getValidatedFile(mockContext, "data.bin", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test
    fun `test valid nested subdirectory filename`() {
        File(sandboxDir, "subdir").mkdirs()
        val result = PathValidation.getValidatedFile(mockContext, "subdir/file.dat", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test
    fun `test valid deeply nested filename`() {
        File(sandboxDir, "a/b/c").mkdirs()
        val result = PathValidation.getValidatedFile(mockContext, "a/b/c/deep.bin", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test
    fun `test dot in filename is allowed`() {
        val result = PathValidation.getValidatedFile(mockContext, "config.v2.json", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test
    fun `test current directory dot is allowed`() {
        val result = PathValidation.getValidatedFile(mockContext, "./data.bin", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test
    fun `test filename with spaces`() {
        val result = PathValidation.getValidatedFile(mockContext, "my file.bin", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }

    @Test
    fun `test filename with unicode characters`() {
        val result = PathValidation.getValidatedFile(mockContext, "データ.bin", sandboxDir)
        assertTrue(result.canonicalPath.startsWith(sandboxDir.canonicalPath))
    }
}
