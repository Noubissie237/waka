package com.propentatech.waka.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.propentatech.waka.model.Currency
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.ui.components.AuthGateSheet
import com.propentatech.waka.ui.components.WakaField
import com.propentatech.waka.ui.components.WakaFormSheet
import com.propentatech.waka.ui.components.WakaTopBar
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    displayCurrency: Currency,
    isPinSet: Boolean,
    biometricEnabled: Boolean,
    biometricAuthenticator: BiometricAuthenticator,
    reauthState: PinAuthUiState,
    backupEvents: Flow<BackupEvent>,
    onDisplayCurrencyChange: (Currency) -> Unit,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onBeginReauth: () -> Unit,
    onReauthDigit: (Char) -> Unit,
    onReauthBackspace: () -> Unit,
    onTryReauthBiometric: (FragmentActivity) -> Unit,
    onExportData: () -> Unit,
    onImportData: (Uri) -> Unit,
    hasFullStorageAccess: () -> Boolean,
) {
    var showSetPin by remember { mutableStateOf(false) }
    var showClearPinConfirm by remember { mutableStateOf(false) }
    var showAuthGate by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showStorageAccessDialog by remember { mutableStateOf(false) }
    var pendingStorageAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val activity = LocalActivity.current as? FragmentActivity
    val context = LocalContext.current

    /** Rien à vérifier tant qu'aucun code PIN n'est configuré : l'action passe directement. */
    fun requireAuth(action: () -> Unit) {
        if (!isPinSet) {
            action()
            return
        }
        pendingAction = action
        onBeginReauth()
        showAuthGate = true
    }

    val storageSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (hasFullStorageAccess()) {
            pendingStorageAction?.invoke()
        }
        pendingStorageAction = null
    }

    /** Le dossier waka/ visible à la racine exige l'accès « tous les fichiers » sur Android 11+. */
    fun requireStorageAccess(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || hasFullStorageAccess()) {
            action()
        } else {
            pendingStorageAction = action
            showStorageAccessDialog = true
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportData)
    }

    LaunchedEffect(backupEvents) {
        backupEvents.collect { event ->
            when (event) {
                is BackupEvent.ExportSuccess -> {
                    resultMessage = "Sauvegarde créée : ${event.file.name}"
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", event.file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Partager la sauvegarde"))
                }
                is BackupEvent.ExportError -> resultMessage = "Échec de l'export : ${event.message}"
                is BackupEvent.ImportSuccess -> resultMessage =
                    "Import réussi : ${event.projectCount} projet(s) et ${event.objectiveCount} objectif(s) " +
                        "ajouté(s) (le reste existait déjà, aucun doublon créé)."
                is BackupEvent.ImportError -> resultMessage = "Échec de l'import : ${event.message}"
            }
        }
    }

    Scaffold(topBar = { WakaTopBar(title = "Paramètres") }) { innerPadding ->
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
                        TextButton(onClick = { requireAuth { showSetPin = true } }) {
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
                                    requireAuth {
                                        if (!enabled || activity == null) {
                                            onBiometricEnabledChange(enabled)
                                        } else {
                                            val availability = biometricAuthenticator.availability(activity)
                                            onBiometricEnabledChange(
                                                availability is BiometricAuthenticator.Availability.Available,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        TextButton(onClick = { requireAuth { showClearPinConfirm = true } }) {
                            Text("Supprimer le code PIN", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Sauvegarde") {
                    Text(
                        "Exporte tous tes projets, objectifs, versements, dépendances et rappels dans un fichier JSON, ou importe une sauvegarde précédente. Protégé par code PIN ou empreinte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = { requireAuth { requireStorageAccess(onExportData) } }) {
                            Text("Exporter")
                        }
                        TextButton(onClick = {
                            requireAuth { importLauncher.launch(arrayOf("application/json")) }
                        }) {
                            Text("Importer")
                        }
                    }
                    Text(
                        if (hasFullStorageAccess()) {
                            "Stocké dans le dossier « waka » à la racine du téléphone."
                        } else {
                            "Stocké dans le dossier « waka » de l'app tant que l'accès à tous les fichiers n'est pas activé."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("Sauvegarde") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK") } },
        )
    }

    if (showAuthGate) {
        AuthGateSheet(
            uiState = reauthState,
            onDigit = onReauthDigit,
            onBackspace = onReauthBackspace,
            onBiometricRequested = onTryReauthBiometric,
            onAuthenticated = {
                showAuthGate = false
                pendingAction?.invoke()
                pendingAction = null
            },
            onDismiss = {
                showAuthGate = false
                pendingAction = null
            },
        )
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

    if (showStorageAccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showStorageAccessDialog = false
                pendingStorageAction = null
            },
            title = { Text("Accès à tous les fichiers") },
            text = {
                Text(
                    "Pour que la sauvegarde soit visible dans un dossier « waka » à la racine du " +
                        "téléphone (et pas caché dans les fichiers de l'app), Waka a besoin de la " +
                        "permission « Autoriser la gestion de tous les fichiers ». Tu peux l'activer " +
                        "dans les réglages du système.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStorageAccessDialog = false
                    storageSettingsLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }) { Text("Ouvrir les réglages") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStorageAccessDialog = false
                    pendingStorageAction = null
                }) { Text("Annuler") }
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
