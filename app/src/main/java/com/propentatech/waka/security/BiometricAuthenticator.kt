package com.propentatech.waka.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Empreinte digitale proposée en priorité si le capteur existe ; le PIN reste toujours le repli. */
class BiometricAuthenticator {

    sealed interface Availability {
        data object Available : Availability
        data object NoHardware : Availability
        data object NoneEnrolled : Availability
        data object Unavailable : Availability
    }

    fun availability(activity: FragmentActivity): Availability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> Availability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NoneEnrolled
            else -> Availability.Unavailable
        }
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
    ): Result = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(Result.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) {
                    continuation.resume(Result.Error(errorCode, errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                // Empreinte non reconnue : le prompt système reste ouvert, l'utilisateur peut réessayer.
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { subtitle?.let { setSubtitle(it) } }
            .setNegativeButtonText("Utiliser le code PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(info)
    }

    sealed interface Result {
        data object Success : Result
        data class Error(val code: Int, val message: String) : Result
    }
}
