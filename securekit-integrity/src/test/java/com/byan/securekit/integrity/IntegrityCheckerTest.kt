package com.byan.securekit.integrity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
}
