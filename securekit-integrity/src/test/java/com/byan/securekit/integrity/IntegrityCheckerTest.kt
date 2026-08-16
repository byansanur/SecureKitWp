package com.byan.securekit.integrity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrityCheckerTest {

    private val integrityChecker = IntegrityChecker()

    @Test
    fun `test integrity checker instantiation`() {
        assertNotNull(integrityChecker)
    }

    @Test
    fun `test emulator detection heuristic method runs without exception`() {
        // Evaluate isEmulator on host JVM environment
        val isEmu = integrityChecker.isEmulator()
        // Should evaluate boolean safely without throwing NullPointerException
        assertNotNull(isEmu)
    }

    @Test
    fun `test native bridge fallback when native library is not present on host JVM`() {
        // On host JVM, .so library isn't loaded so NativeSecurityBridge.isNativeAvailable should be false
        assertFalse(NativeSecurityBridge.isNativeAvailable)
        assertFalse(NativeSecurityBridge.safeIsDeviceRooted())
        assertFalse(NativeSecurityBridge.safeIsHookingDetected())
        assertFalse(NativeSecurityBridge.safeIsAdvancedRootDetected())
        assertFalse(NativeSecurityBridge.safeIsNativeEmulatorDetected())
    }

    @Test
    fun `test emulator detection heuristic returns true when qemu property is set`() {
        System.setProperty("ro.kernel.qemu", "1")
        try {
            assertTrue(integrityChecker.isEmulator())
        } finally {
            System.clearProperty("ro.kernel.qemu")
        }
    }

    @Test
    fun `test isDeveloperModeEnabled handles exception gracefully and returns false`() {
        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        // Null content resolver will trigger exception in Settings.Global
        org.mockito.Mockito.`when`(mockContext.contentResolver).thenReturn(null)

        val result = integrityChecker.isDeveloperModeEnabled(mockContext)
        assertFalse(result)
    }

    @Test
    fun `test isAdbEnabled handles exception gracefully and returns false`() {
        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        org.mockito.Mockito.`when`(mockContext.contentResolver).thenReturn(null)

        val result = integrityChecker.isAdbEnabled(mockContext)
        assertFalse(result)
    }

    @Test
    fun `test isDeviceLockEnabled returns false when KeyguardManager is null`() {
        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        org.mockito.Mockito.`when`(mockContext.getSystemService(android.content.Context.KEYGUARD_SERVICE)).thenReturn(null)

        val result = integrityChecker.isDeviceLockEnabled(mockContext)
        assertFalse(result)
    }

    @Test
    fun `test custom logger captures messages in IntegrityChecker`() {
        val loggedMessages = mutableListOf<String>()
        val customLogger = object : com.byan.securekit.core.SecurityLogger {
            override fun d(tag: String, message: String) {
                loggedMessages.add("[$tag] $message")
            }
            override fun e(tag: String, message: String, throwable: Throwable?) {
                loggedMessages.add("[ERROR] [$tag] $message")
            }
        }

        val checkerWithLogger = IntegrityChecker(logger = customLogger)
        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        org.mockito.Mockito.`when`(mockContext.contentResolver).thenReturn(null)

        checkerWithLogger.isDeveloperModeEnabled(mockContext)

        assertTrue(loggedMessages.any { it.contains("Error checking Developer Mode status") })
    }
}
