package com.propentatech.waka.security

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 4

data class PinAuthUiState(
    val pin: String = "",
    val error: String? = null,
    val isLockedOut: Boolean = false,
    val lockoutUntilMillis: Long? = null,
    val pinConfigured: Boolean = true,
    val authenticated: Boolean = false,
)

/**
 * Vérification PIN/empreinte partagée entre le verrouillage d'un projet privé et la
 * ré-authentification dans les Paramètres : même code PIN, même protection anti-brute-force
 * (5 échecs -> verrouillage temporaire), pour ne pas dupliquer cette logique sensible.
 */
class PinAuthController(
    private val scope: CoroutineScope,
    private val securityPreferences: SecurityPreferences,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val biometricPromptTitle: String,
) {
    private val _uiState = MutableStateFlow(PinAuthUiState(pinConfigured = securityPreferences.isPinSet()))
    val uiState: StateFlow<PinAuthUiState> = _uiState.asStateFlow()

    init {
        refreshLockout()
    }

    /** À appeler à chaque nouvelle demande de ré-authentification, pour repartir d'un état propre. */
    fun reset() {
        _uiState.value = PinAuthUiState(pinConfigured = securityPreferences.isPinSet())
        refreshLockout()
    }

    fun isBiometricAvailable(activity: FragmentActivity): Boolean =
        securityPreferences.isBiometricEnabled() &&
            biometricAuthenticator.availability(activity) is BiometricAuthenticator.Availability.Available

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.isLockedOut || !state.pinConfigured || state.pin.length >= PIN_LENGTH) return
        val updated = state.pin + digit
        _uiState.update { it.copy(pin = updated, error = null) }
        if (updated.length == PIN_LENGTH) submit(updated)
    }

    fun onBackspace() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1), error = null) }
    }

    fun tryBiometric(activity: FragmentActivity) {
        scope.launch {
            when (biometricAuthenticator.authenticate(activity, title = biometricPromptTitle)) {
                is BiometricAuthenticator.Result.Success -> {
                    securityPreferences.resetFailedAttempts()
                    _uiState.update { it.copy(authenticated = true) }
                }
                is BiometricAuthenticator.Result.Error -> {
                    // Échec/annulation biométrique : pas de pénalité, l'utilisateur retombe sur le PIN.
                }
            }
        }
    }

    private fun submit(pin: String) {
        if (securityPreferences.verifyPin(pin)) {
            securityPreferences.resetFailedAttempts()
            _uiState.update { it.copy(pin = "", authenticated = true, error = null) }
            return
        }
        securityPreferences.recordFailedAttempt()
        _uiState.update { it.copy(pin = "", error = "Code incorrect") }
        refreshLockout()
    }

    private fun refreshLockout() {
        val lockout = securityPreferences.currentLockout()
        _uiState.update { it.copy(isLockedOut = lockout != null, lockoutUntilMillis = lockout?.untilMillis) }
        if (lockout != null) waitForLockoutToExpire(lockout.untilMillis)
    }

    private fun waitForLockoutToExpire(untilMillis: Long) {
        scope.launch {
            val remaining = untilMillis - System.currentTimeMillis()
            if (remaining > 0) delay(remaining)
            securityPreferences.resetFailedAttempts()
            _uiState.update { it.copy(isLockedOut = false, lockoutUntilMillis = null) }
        }
    }
}
