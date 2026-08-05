package com.byan.securekit.integrity

/**
 * Bridge penghubung antara Kotlin dan library native C++ (secure_kit.so).
 * Menggunakan graceful fallback jika library native tidak dapat dimuat.
 */
internal object NativeSecurityBridge {

    val isNativeAvailable: Boolean

    init {
        isNativeAvailable = try {
            System.loadLibrary("secure_kit")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    external fun isDeviceRooted(): Boolean
    external fun isHookingDetected(): Boolean
    external fun isAdvancedRootDetected(): Boolean
    external fun isNativeEmulatorDetected(): Boolean

    fun safeIsDeviceRooted(): Boolean {
        return if (isNativeAvailable) isDeviceRooted() else false
    }

    fun safeIsHookingDetected(): Boolean {
        return if (isNativeAvailable) isHookingDetected() else false
    }

    fun safeIsAdvancedRootDetected(): Boolean {
        return if (isNativeAvailable) isAdvancedRootDetected() else false
    }

    fun safeIsNativeEmulatorDetected(): Boolean {
        return if (isNativeAvailable) isNativeEmulatorDetected() else false
    }
}
