package com.propentatech.waka.ui.screens.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.model.Currency
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.PinAuthController
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val securityPreferences: SecurityPreferences,
    biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    val displayCurrency: StateFlow<Currency> = appPreferences.defaultDisplayCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    private val _isPinSet = MutableStateFlow(securityPreferences.isPinSet())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(securityPreferences.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    /** Ré-authentification requise avant de modifier/supprimer le PIN ou de (dés)activer la biométrie. */
    private val reauthController = PinAuthController(
        scope = viewModelScope,
        securityPreferences = securityPreferences,
        biometricAuthenticator = biometricAuthenticator,
        biometricPromptTitle = "Confirmer ton identité",
    )
    val reauthState: StateFlow<PinAuthUiState> = reauthController.uiState

    fun beginReauth() = reauthController.reset()

    fun isReauthBiometricAvailable(activity: FragmentActivity): Boolean =
        reauthController.isBiometricAvailable(activity)

    fun onReauthDigit(digit: Char) = reauthController.onDigit(digit)

    fun onReauthBackspace() = reauthController.onBackspace()

    fun tryReauthBiometric(activity: FragmentActivity) = reauthController.tryBiometric(activity)

    fun setDisplayCurrency(currency: Currency) {
        viewModelScope.launch { appPreferences.setDefaultDisplayCurrency(currency) }
    }

    fun setPin(pin: String) {
        securityPreferences.setPin(pin)
        _isPinSet.value = true
    }

    fun clearPin() {
        securityPreferences.clearPin()
        _isPinSet.value = false
        _biometricEnabled.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityPreferences.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
    }
}
