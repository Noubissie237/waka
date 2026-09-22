package com.propentatech.waka.ui.screens.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.PinAuthController
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.security.SecurityPreferences
import kotlinx.coroutines.flow.StateFlow

class LockViewModel(
    securityPreferences: SecurityPreferences,
    biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    private val controller = PinAuthController(
        scope = viewModelScope,
        securityPreferences = securityPreferences,
        biometricAuthenticator = biometricAuthenticator,
        biometricPromptTitle = "Déverrouiller le projet",
    )

    val uiState: StateFlow<PinAuthUiState> = controller.uiState

    fun isBiometricAvailable(activity: FragmentActivity): Boolean = controller.isBiometricAvailable(activity)

    fun onDigit(digit: Char) = controller.onDigit(digit)

    fun onBackspace() = controller.onBackspace()

    fun tryBiometric(activity: FragmentActivity) = controller.tryBiometric(activity)
}
