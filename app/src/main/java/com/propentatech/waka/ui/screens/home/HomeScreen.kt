package com.propentatech.waka.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.model.Currency
import com.propentatech.waka.ui.components.WakaField
import com.propentatech.waka.ui.components.WakaFormSheet
import com.propentatech.waka.ui.components.WakaSegmentedControl
import com.propentatech.waka.ui.format.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDisplayCurrencyChange: (Currency) -> Unit,
    onCreateProject: (title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) -> Unit,
    onUpdateProject: (ProjectItem, title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) -> Unit,
    onDeleteProject: (ProjectItem) -> Unit,
    onProjectClick: (ProjectItem) -> Unit,
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectItem?>(null) }
    var deletingProject by remember { mutableStateOf<ProjectItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes projets") },
                actions = {
                    CurrencyToggle(selected = uiState.displayCurrency, onSelect = onDisplayCurrencyChange)
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nouveau projet")
            }
        },
    ) { innerPadding ->
        if (uiState.projects.isEmpty() && !uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Aucun projet pour l'instant. Touchez + pour créer le premier.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.projects, key = { it.item.id }) { entry ->
                    ProjectCard(
                        entry = entry,
                        displayCurrency = uiState.displayCurrency,
                        onClick = { onProjectClick(entry.item) },
                        onEdit = { editingProject = entry.item },
                        onDelete = { deletingProject = entry.item },
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        ProjectFormSheet(
            existing = null,
            onDismiss = { showCreateSheet = false },
            onConfirm = { title, amount, currency, isPrivate ->
                onCreateProject(title, amount, currency, isPrivate)
                showCreateSheet = false
            },
        )
    }

    editingProject?.let { project ->
        ProjectFormSheet(
            existing = project,
            onDismiss = { editingProject = null },
            onConfirm = { title, amount, currency, isPrivate ->
                onUpdateProject(project, title, amount, currency, isPrivate)
                editingProject = null
            },
        )
    }

    deletingProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("Supprimer « ${project.title} » ?") },
            text = { Text("Toutes ses tâches et versements seront supprimés définitivement.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProject(project)
                    deletingProject = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingProject = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun ProjectCard(
    entry: ProjectListEntry,
    displayCurrency: Currency,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.item.isPrivate) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Projet privé",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(entry.item.title, style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.progress.targetAmount != null) {
                        Text(
                            "${(entry.progress.percent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Modifier") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = { showMenu = false; onEdit() },
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { showMenu = false; onDelete() },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (entry.progress.targetAmount != null && entry.progress.currency != null) {
                LinearProgressIndicator(
                    progress = { entry.progress.percent },
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatMoney(entry.progress.raised, entry.progress.currency)} / " +
                        formatMoney(entry.progress.targetAmount, entry.progress.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    if (entry.progress.isCompleted) "Terminé" else "En cours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CurrencyToggle(selected: Currency, onSelect: (Currency) -> Unit) {
    Row(modifier = Modifier.padding(end = 12.dp)) {
        Currency.entries.forEach { currency ->
            FilterChip(
                selected = currency == selected,
                onClick = { onSelect(currency) },
                label = { Text(currency.name) },
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun ProjectFormSheet(
    existing: ProjectItem?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember { mutableStateOf(existing?.targetAmount?.let { formatAmountInput(it) }.orEmpty()) }
    var currency by remember { mutableStateOf(existing?.currency ?: Currency.EUR) }
    var isPrivate by remember { mutableStateOf(existing?.isPrivate ?: false) }

    WakaFormSheet(
        title = if (existing == null) "Nouveau projet" else "Modifier le projet",
        subtitle = "Un objectif à suivre, chiffré ou non.",
        onDismiss = onDismiss,
        primaryLabel = if (existing == null) "Créer" else "Enregistrer",
        primaryEnabled = title.isNotBlank(),
        onPrimaryClick = {
            val amount = amountText.toDoubleOrNull()
            onConfirm(title, amount, if (amount != null) currency else null, isPrivate)
        },
    ) {
        WakaField(label = "Titre", value = title, onValueChange = { title = it }, placeholder = "Expatriation Paris")
        Spacer(Modifier.height(18.dp))
        WakaField(
            label = "Montant cible (optionnel)",
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "0",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(14.dp))
        WakaSegmentedControl(
            options = Currency.entries,
            selected = currency,
            onSelect = { currency = it },
            label = { it.name },
        )
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Projet privé", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Protégé par code PIN ou empreinte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
        }
    }
}

private fun formatAmountInput(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
