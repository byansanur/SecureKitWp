package com.byan.securekit.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureCharArrayTest {

    @Test
    fun `test obfuscation and decryption correctness`() {
        val original = charArrayOf('S', 'e', 'c', 'u', 'r', 'e', '1', '2', '3')
        val secureCharArray = SecureCharArray(original)

        secureCharArray.useClearText { decrypted ->
            assertEquals("Secure123", String(decrypted))
        }
    }

    @Test
    fun `test original input array is wiped immediately upon construction`() {
        val input = charArrayOf('P', 'A', 'S', 'S', 'W', 'O', 'R', 'D')
        SecureCharArray(input)

        // Verify input array was zeroed out to '\u0000'
        for (char in input) {
            assertEquals('\u0000', char)
        }
    }

    @Test
    fun `test temporary clear text array is wiped after use block completes`() {
        val original = charArrayOf('T', 'O', 'K', 'E', 'N')
        val secureCharArray = SecureCharArray(original)

        var capturedTempRef: CharArray? = null

        secureCharArray.useClearText { temp ->
            capturedTempRef = temp
            assertEquals('T', temp[0])
        }

        // After lambda block completes, captured reference array elements should be '\u0000'
        capturedTempRef?.let { temp ->
            for (char in temp) {
                assertEquals('\u0000', char)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `test access after close throws IllegalStateException`() {
        val original = charArrayOf('1', '2', '3', '4')
        val secureCharArray = SecureCharArray(original)

        secureCharArray.close()

        // Should throw IllegalStateException
        secureCharArray.useClearText { }
    }

    @Test
    fun `test double close safety`() {
        val original = charArrayOf('A', 'B', 'C')
        val secureCharArray = SecureCharArray(original)

        // Calling close multiple times should be safe and idempotent
        secureCharArray.close()
        secureCharArray.close()
        secureCharArray.clear()
    }

    @Test
    fun `test empty input array handling`() {
        val input = charArrayOf()
        val secureCharArray = SecureCharArray(input)

        secureCharArray.useClearText { decrypted ->
            assertEquals(0, decrypted.size)
        }
    }
}
