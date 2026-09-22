package com.propentatech.waka.ui.screens.settings

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.backup.BackupRepository
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.model.Currency
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.PinAuthController
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.security.SecurityPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface BackupEvent {
    data class ExportSuccess(val file: File) : BackupEvent
    data class ExportError(val message: String) : BackupEvent
    data class ImportSuccess(val projectCount: Int, val objectiveCount: Int) : BackupEvent
    data class ImportError(val message: String) : BackupEvent
}

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val securityPreferences: SecurityPreferences,
    private val backupRepository: BackupRepository,
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

    fun hasFullStorageAccess(): Boolean = backupRepository.hasFullStorageAccess()

    private val _backupEvents = Channel<BackupEvent>(Channel.BUFFERED)
    val backupEvents: Flow<BackupEvent> = _backupEvents.receiveAsFlow()

    fun exportData() {
        viewModelScope.launch {
            try {
                val file = backupRepository.exportToFile()
                _backupEvents.send(BackupEvent.ExportSuccess(file))
            } catch (e: Exception) {
                _backupEvents.send(BackupEvent.ExportError(e.message ?: "Échec de l'export."))
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val result = backupRepository.importFromUri(uri)
                _backupEvents.send(BackupEvent.ImportSuccess(result.projectCount, result.objectiveCount))
            } catch (e: Exception) {
                _backupEvents.send(BackupEvent.ImportError(e.message ?: "Fichier invalide ou illisible."))
            }
        }
    }
}
