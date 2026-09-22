package com.propentatech.waka.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.model.Currency
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
) : ViewModel() {

    val displayCurrency: StateFlow<Currency> = appPreferences.defaultDisplayCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.EUR)

    private val _isPinSet = MutableStateFlow(securityPreferences.isPinSet())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(securityPreferences.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

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
