package com.byan.securekit.integrity

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.provider.Settings
import com.byan.securekit.core.SecureResult
import com.byan.securekit.core.SecurityLogger
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest

/**
 * Modul untuk memeriksa integritas perangkat, sistem operasi, dan runtime (Root, Hooking, Emulator, Play Integrity).
 */
class IntegrityChecker(
    private val logger: SecurityLogger = SecurityLogger.Silent
) {

    /**
     * Memeriksa apakah perangkat memiliki status root (Java path & Native C++ checks).
     */
    fun isRooted(): Boolean {
        return NativeSecurityBridge.safeIsDeviceRooted() || NativeSecurityBridge.safeIsAdvancedRootDetected()
    }

    /**
     * Memeriksa keberadaan framework hooking (Frida, Xposed, Substrate).
     */
    fun isHookingDetected(): Boolean {
        return NativeSecurityBridge.safeIsHookingDetected()
    }

    /**
     * Memeriksa apakah aplikasi berjalan di dalam Emulator.
     */
    fun isEmulator(): Boolean {
        val qemu = System.getProperty("ro.kernel.qemu")

        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        val board = Build.BOARD.orEmpty()
        val model = Build.MODEL.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val bootloader = Build.BOOTLOADER.orEmpty()

        return (brand.startsWith("generic") && device.startsWith("generic"))
                || fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || board.contains("goldfish")
                || board.contains("ranchu")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.lowercase().contains("droid4x")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
                || product.contains("simulator")
                || board.lowercase().contains("nox")
                || bootloader.lowercase().contains("nox")
                || hardware.lowercase().contains("nox")
                || product.lowercase().contains("nox")
                || qemu == "1"
                || NativeSecurityBridge.safeIsNativeEmulatorDetected()
    }

    /**
     * Memeriksa status Developer Options di sistem.
     */
    fun isDeveloperModeEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        } catch (e: Exception) {
            logger.e("IntegrityChecker", "Error checking Developer Mode status", e)
            false
        }
    }

    /**
     * Memeriksa status USB Debugging (ADB).
     */
    fun isAdbEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
        } catch (e: Exception) {
            logger.e("IntegrityChecker", "Error checking ADB status", e)
            false
        }
    }

    /**
     * Memeriksa apakah perangkat dilindungi oleh Kunci Layar (PIN/Password/Biometrik).
     */
    fun isDeviceLockEnabled(context: Context): Boolean {
        return try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.isDeviceSecure ?: false
        } catch (e: Exception) {
            logger.e("IntegrityChecker", "Error checking Device Lock status", e)
            false
        }
    }

    /**
     * Pengecekan komprehensif untuk memastikan lingkungan aman bagi aplikasi finansial.
     */
    fun isEnvironmentSafe(context: Context): Boolean {
        val rooted = isRooted()
        val hooked = isHookingDetected()
        val emu = isEmulator()
        val debug = Debug.isDebuggerConnected()
        val devMode = isDeveloperModeEnabled(context)
        val adb = isAdbEnabled(context)
        val lock = isDeviceLockEnabled(context)

        logger.d("IntegrityChecker", "Rooted: $rooted, Hooked: $hooked, Emulator: $emu, Debugger: $debug, DevMode: $devMode, ADB: $adb, LockEnabled: $lock")

        return !rooted && !hooked && !emu && !debug && !devMode && !adb && lock
    }

    /**
     * Meminta token attestation dari Google Play Integrity API secara terstruktur ([SecureResult]).
     */
    fun requestIntegrityToken(
        context: Context,
        nonce: String,
        onResult: (SecureResult<String>) -> Unit
    ) {
        try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val request = IntegrityTokenRequest.builder().setNonce(nonce).build()

            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    onResult(SecureResult.Success(response.token()))
                }
                .addOnFailureListener { exception ->
                    logger.e("IntegrityChecker", "Play Integrity token request failed", exception)
                    onResult(SecureResult.Error(exception))
                }
        } catch (e: Exception) {
            logger.e("IntegrityChecker", "IntegrityManager error", e)
            onResult(SecureResult.Error(e))
        }
    }
}
