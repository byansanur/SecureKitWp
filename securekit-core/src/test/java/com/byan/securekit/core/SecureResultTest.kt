package com.byan.securekit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureResultTest {

    @Test
    fun `test success result behavior`() {
        val result: SecureResult<String> = SecureResult.Success("SecretValue")

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("SecretValue", result.getOrNull())
        assertEquals("SecretValue", result.getOrDefault("Default"))
    }

    @Test
    fun `test error result behavior`() {
        val exception = RuntimeException("Encryption error")
        val result: SecureResult<String> = SecureResult.Error(exception, "Custom message")

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals("Default", result.getOrDefault("Default"))
        assertEquals("Custom message", (result as SecureResult.Error).message)
    }
}
