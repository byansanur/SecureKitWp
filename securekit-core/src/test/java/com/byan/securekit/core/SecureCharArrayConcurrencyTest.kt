package com.byan.securekit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test keamanan memori SecureCharArray terhadap exception safety,
 * race condition close/use, dan karakter non-ASCII (CWE-316 / OWASP M1).
 */
class SecureCharArrayConcurrencyTest {

    // --- Exception Safety: buffer HARUS di-zero-fill meski exception terjadi di block ---

    @Test
    fun `test buffer is zeroed even when exception is thrown inside useClearText block`() {
        val original = charArrayOf('S', 'E', 'C', 'R', 'E', 'T')
        val secureArray = SecureCharArray(original)

        var capturedRef: CharArray? = null

        try {
            secureArray.useClearText { temp ->
                capturedRef = temp
                // Verify data is accessible before exception
                assertEquals('S', temp[0])
                throw RuntimeException("Simulated crash inside useClearText")
            }
        } catch (e: RuntimeException) {
            // Expected
        }

        // Verify: temporary buffer MUST be zeroed despite the exception
        capturedRef?.let { temp ->
            for (i in temp.indices) {
                assertEquals(
                    "Char at index $i should be zeroed after exception",
                    '\u0000', temp[i]
                )
            }
        } ?: fail("capturedRef should not be null")
    }

    @Test
    fun `test buffer is zeroed even when Error is thrown inside useClearText block`() {
        val original = charArrayOf('P', 'I', 'N')
        val secureArray = SecureCharArray(original)

        var capturedRef: CharArray? = null

        try {
            secureArray.useClearText { temp ->
                capturedRef = temp
                throw OutOfMemoryError("Simulated OOM inside useClearText")
            }
        } catch (e: OutOfMemoryError) {
            // Expected
        }

        capturedRef?.let { temp ->
            for (char in temp) {
                assertEquals('\u0000', char)
            }
        } ?: fail("capturedRef should not be null")
    }

    // --- Race Condition: close() vs useClearText() ---

    @Test
    fun `test close then useClearText throws IllegalStateException`() {
        val secureArray = SecureCharArray(charArrayOf('A', 'B', 'C'))
        secureArray.close()

        try {
            secureArray.useClearText { /* should not reach here */ }
            fail("Expected IllegalStateException after close()")
        } catch (e: IllegalStateException) {
            // Expected: "SecureCharArray sudah dibersihkan (cleared/closed)!"
            assertTrue(e.message?.contains("dibersihkan") == true || e.message?.contains("cleared") == true)
        }
    }

    @Test
    fun `test concurrent close and useClearText does not leak data`() {
        val iterations = 100
        val dataLeakDetected = AtomicBoolean(false)
        val exceptionsCount = AtomicInteger(0)

        for (iter in 0 until iterations) {
            val secureArray = SecureCharArray(charArrayOf('X', 'Y', 'Z'))
            val barrier = CyclicBarrier(2)
            val latch = CountDownLatch(2)

            // Thread 1: tries to close
            Thread {
                try {
                    barrier.await()
                    secureArray.close()
                } catch (_: Exception) {
                    // Expected
                } finally {
                    latch.countDown()
                }
            }.start()

            // Thread 2: tries to useClearText
            Thread {
                try {
                    barrier.await()
                    secureArray.useClearText { temp ->
                        // If we get here, data should be valid (not half-zeroed)
                        val str = String(temp)
                        if (str != "XYZ" && str != "\u0000\u0000\u0000") {
                            dataLeakDetected.set(true)
                        }
                    }
                } catch (e: IllegalStateException) {
                    // Expected if close() won the race
                    exceptionsCount.incrementAndGet()
                } catch (_: Exception) {
                    // Other exceptions acceptable
                } finally {
                    latch.countDown()
                }
            }.start()

            latch.await()
        }

        // No partial data should have leaked
        assertTrue(
            "No partial/corrupted data should be observable in race condition",
            !dataLeakDetected.get()
        )
    }

    // --- Unicode / Multibyte Character Support ---

    @Test
    fun `test unicode characters are preserved correctly`() {
        val unicodeChars = charArrayOf('日', '本', '語', '字')
        val secureArray = SecureCharArray(unicodeChars)

        // Verify original wiped
        for (char in unicodeChars) {
            assertEquals('\u0000', char)
        }

        secureArray.useClearText { temp ->
            assertEquals('日', temp[0])
            assertEquals('本', temp[1])
            assertEquals('語', temp[2])
            assertEquals('字', temp[3])
        }
    }

    @Test
    fun `test emoji and special characters roundtrip`() {
        val special = charArrayOf('✅', '❌', '⚠', '™', '©')
        val secureArray = SecureCharArray(special)

        secureArray.useClearText { temp ->
            assertEquals('✅', temp[0])
            assertEquals('❌', temp[1])
            assertEquals('⚠', temp[2])
            assertEquals('™', temp[3])
            assertEquals('©', temp[4])
        }
    }

    @Test
    fun `test single character array`() {
        val secureArray = SecureCharArray(charArrayOf('X'))

        secureArray.useClearText { temp ->
            assertEquals(1, temp.size)
            assertEquals('X', temp[0])
        }
    }

    @Test
    fun `test large array 10000 chars is zeroed correctly`() {
        val largeArray = CharArray(10000) { 'A' + (it % 26) }
        val secureArray = SecureCharArray(largeArray)

        // Verify original wiped
        for (char in largeArray) {
            assertEquals('\u0000', char)
        }

        var capturedRef: CharArray? = null
        secureArray.useClearText { temp ->
            capturedRef = temp
            assertEquals(10000, temp.size)
            assertEquals('A', temp[0])
            assertEquals('Z', temp[25])
        }

        // Verify zeroed after use
        capturedRef?.let { temp ->
            for (char in temp) {
                assertEquals('\u0000', char)
            }
        }
    }

    // --- Multiple sequential useClearText calls ---

    @Test
    fun `test multiple sequential useClearText calls all succeed and zero`() {
        val secureArray = SecureCharArray(charArrayOf('1', '2', '3', '4'))

        repeat(50) {
            var capturedRef: CharArray? = null
            secureArray.useClearText { temp ->
                capturedRef = temp
                assertEquals("1234", String(temp))
            }
            capturedRef?.let { temp ->
                for (char in temp) {
                    assertEquals('\u0000', char)
                }
            }
        }
    }
}
