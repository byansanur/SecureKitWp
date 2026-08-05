package com.byan.securekitwp.data.repository

import android.content.Context
import com.byan.securekit.core.SecureResult
import com.byan.securekit.crypto.SecureVault
import com.byan.securekit.integrity.IntegrityChecker
import com.byan.securekit.network.NetworkArmor
import com.byan.securekitwp.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Arrays

data class SecurityStatusState(
    val isRooted: Boolean,
    val isHooked: Boolean,
    val isEmulator: Boolean,
    val isDebuggerAttached: Boolean
)

data class NetworkSecurityState(
    val isProxyEnabled: Boolean,
    val isVpnEnabled: Boolean
)

open class SecurityRepository(private val context: Context) {

    // Singleton Database instance initialized securely
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val integrityChecker = IntegrityChecker()
    private val networkArmor = NetworkArmor()
    private val secureVault = SecureVault()

    open suspend fun checkSecurityEnvironment(): SecurityStatusState = withContext(Dispatchers.IO) {
        SecurityStatusState(
            isRooted = integrityChecker.isRooted(),
            isHooked = integrityChecker.isHookingDetected(),
            isEmulator = integrityChecker.isEmulator(),
            isDebuggerAttached = integrityChecker.isDeveloperModeEnabled(context)
        )
    }

    open suspend fun checkNetworkSecurity(): NetworkSecurityState = withContext(Dispatchers.IO) {
        NetworkSecurityState(
            isProxyEnabled = networkArmor.isProxyActive(context),
            isVpnEnabled = networkArmor.isVpnActive(context)
        )
    }

    suspend fun saveEncryptedToken(token: String): SecureResult<Unit> = withContext(Dispatchers.IO) {
        secureVault.saveString(context, "user_token", token)
    }

    suspend fun readEncryptedToken(): SecureResult<String?> = withContext(Dispatchers.IO) {
        val result = secureVault.getString(context, "user_token")
        if (result is SecureResult.Success) {
            SecureResult.Success(result.data)
        } else {
            result as SecureResult.Error
        }
    }

    suspend fun saveSecurePin(pin: CharArray): SecureResult<Unit> = withContext(Dispatchers.IO) {
        val pinStr = String(pin)
        val result = secureVault.saveString(context, "user_pin", pinStr)
        // Zero-fill pin after encryption
        Arrays.fill(pin, '0')
        result
    }

    suspend fun verifySecurePin(inputPin: CharArray): Boolean = withContext(Dispatchers.IO) {
        val result = secureVault.getString(context, "user_pin")
        var isMatch = false
        if (result is SecureResult.Success) {
            isMatch = String(inputPin) == result.data
        }
        Arrays.fill(inputPin, '0') // Clear memory
        isMatch
    }
}
