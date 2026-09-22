package com.propentatech.waka.ui.screens.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.propentatech.waka.model.Currency
import com.propentatech.waka.ui.components.WakaField
import com.propentatech.waka.ui.components.WakaFormSheet
import com.propentatech.waka.security.BiometricAuthenticator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    displayCurrency: Currency,
    isPinSet: Boolean,
    biometricEnabled: Boolean,
    biometricAuthenticator: BiometricAuthenticator,
    onDisplayCurrencyChange: (Currency) -> Unit,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
) {
    var showSetPin by remember { mutableStateOf(false) }
    var showClearPinConfirm by remember { mutableStateOf(false) }
    val activity = LocalActivity.current as? FragmentActivity

    Scaffold(topBar = { TopAppBar(title = { Text("Paramètres") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsSection(title = "Affichage") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Devise d'affichage")
                        Row {
                            Currency.entries.forEach { currency ->
                                FilterChip(
                                    selected = currency == displayCurrency,
                                    onClick = { onDisplayCurrencyChange(currency) },
                                    label = { Text(currency.name) },
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Taux de change fixe : 1 € = 655 XAF (non modifiable)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection(title = "Confidentialité") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (isPinSet) "Code PIN configuré" else "Aucun code PIN")
                        TextButton(onClick = { showSetPin = true }) {
                            Text(if (isPinSet) "Modifier" else "Configurer")
                        }
                    }
                    if (isPinSet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Déverrouillage biométrique")
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled || activity == null) {
                                        onBiometricEnabledChange(enabled)
                                    } else {
                                        val availability = biometricAuthenticator.availability(activity)
                                        onBiometricEnabledChange(
                                            availability is BiometricAuthenticator.Availability.Available,
                                        )
                                    }
                                },
                            )
                        }
                        TextButton(onClick = { showClearPinConfirm = true }) {
                            Text("Supprimer le code PIN", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onConfirm = { pin ->
                onSetPin(pin)
                showSetPin = false
            },
        )
    }

    if (showClearPinConfirm) {
        AlertDialog(
            onDismissRequest = { showClearPinConfirm = false },
            title = { Text("Supprimer le code PIN ?") },
            text = { Text("Les projets privés ne pourront plus être ouverts tant qu'un nouveau code n'est pas configuré.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearPin()
                    showClearPinConfirm = false
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearPinConfirm = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val mismatch = confirmPin.isNotEmpty() && pin != confirmPin

    WakaFormSheet(
        title = "Code PIN",
        subtitle = "4 chiffres pour protéger tes projets privés.",
        onDismiss = onDismiss,
        primaryLabel = "Enregistrer",
        primaryEnabled = pin.length == 4 && pin == confirmPin,
        onPrimaryClick = { onConfirm(pin) },
    ) {
        WakaField(
            label = "Nouveau code",
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
            placeholder = "• • • •",
            keyboardType = KeyboardType.NumberPassword,
        )
        Spacer(Modifier.height(18.dp))
        WakaField(
            label = "Confirmer",
            value = confirmPin,
            onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(4) },
            placeholder = "• • • •",
            keyboardType = KeyboardType.NumberPassword,
        )
        if (mismatch) {
            Spacer(Modifier.height(8.dp))
            Text("Les codes ne correspondent pas", color = MaterialTheme.colorScheme.error)
        }
    }
}
