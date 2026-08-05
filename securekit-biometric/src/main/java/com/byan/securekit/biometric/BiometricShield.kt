package com.byan.securekit.biometric

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.byan.securekit.core.SecureResult

/**
 * Wrapper terstruktur untuk BiometricPrompt (Otentikasi Sidik Jari / Wajah).
 */
class BiometricShield {

    /**
     * Menampilkan prompt otentikasi biometrik sistem.
     */
    fun showPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (SecureResult<Unit>) -> Unit
    ) {
        authenticateInternal(activity, title, subtitle, cryptoObject = null) { result ->
            when (result) {
                is SecureResult.Success -> onResult(SecureResult.Success(Unit))
                is SecureResult.Error -> onResult(result)
            }
        }
    }

    /**
     * Menampilkan prompt biometrik yang terikat secara kriptografis ke [BiometricPrompt.CryptoObject] (Cipher/Signature/Mac).
     * Memastikan dekripsi kunci di TEE/StrongBox terproteksi oleh otentikasi biometrik hardware.
     */
    fun showPromptWithCryptoObject(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject,
        onResult: (SecureResult<BiometricPrompt.CryptoObject?>) -> Unit
    ) {
        authenticateInternal(activity, title, subtitle, cryptoObject) { result ->
            onResult(result)
        }
    }

    private fun authenticateInternal(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onResult: (SecureResult<BiometricPrompt.CryptoObject?>) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(SecureResult.Error(Exception("Biometric Error ($errorCode): $errString")))
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(SecureResult.Success(result.cryptoObject))
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(SecureResult.Error(Exception("Autentikasi biometrik tidak cocok.")))
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Batal")
            .build()

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
