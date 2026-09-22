package com.propentatech.waka.ui.screens.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.SecurityPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 4

data class LockUiState(
    val pin: String = "",
    val error: String? = null,
    val isLockedOut: Boolean = false,
    val lockoutUntilMillis: Long? = null,
    val pinConfigured: Boolean = true,
    val unlocked: Boolean = false,
)

class LockViewModel(
    private val securityPreferences: SecurityPreferences,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LockUiState(pinConfigured = securityPreferences.isPinSet()),
    )
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
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
        viewModelScope.launch {
            when (biometricAuthenticator.authenticate(activity, title = "Déverrouiller le projet")) {
                is BiometricAuthenticator.Result.Success -> {
                    securityPreferences.resetFailedAttempts()
                    _uiState.update { it.copy(unlocked = true) }
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
            _uiState.update { it.copy(pin = "", unlocked = true, error = null) }
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
        viewModelScope.launch {
            val remaining = untilMillis - System.currentTimeMillis()
            if (remaining > 0) delay(remaining)
            securityPreferences.resetFailedAttempts()
            _uiState.update { it.copy(isLockedOut = false, lockoutUntilMillis = null) }
        }
    }
}
