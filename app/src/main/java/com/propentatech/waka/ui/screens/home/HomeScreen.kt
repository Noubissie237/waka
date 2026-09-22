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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.domain.ProjectAggregator
import com.propentatech.waka.model.Currency
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.ui.components.AuthGateSheet
import com.propentatech.waka.ui.components.ProjectFormSheet
import com.propentatech.waka.ui.components.WakaTopBar
import com.propentatech.waka.ui.format.daysUntil
import com.propentatech.waka.ui.format.formatDaysRemaining
import com.propentatech.waka.ui.format.formatMoney
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    revealedProjectIds: Set<Long>,
    reauthState: PinAuthUiState,
    onDisplayCurrencyChange: (Currency) -> Unit,
    onCreateProject: (title: String, deadlineAt: Long?, isPrivate: Boolean) -> Unit,
    onUpdateProject: (ProjectItem, title: String, deadlineAt: Long?, isPrivate: Boolean) -> Unit,
    onDeleteProject: (ProjectItem) -> Unit,
    onProjectClick: (ProjectItem) -> Unit,
    onBeginReauth: () -> Unit,
    onReauthDigit: (Char) -> Unit,
    onReauthBackspace: () -> Unit,
    onTryReauthBiometric: (FragmentActivity) -> Unit,
    onRevealProject: (Long) -> Unit,
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectItem?>(null) }
    var deletingProject by remember { mutableStateOf<ProjectItem?>(null) }
    var showAuthGate by remember { mutableStateOf(false) }
    var pendingRevealId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            WakaTopBar(
                title = "Mes projets",
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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aucun projet pour l'instant. Touchez + pour créer le premier.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
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
                        isRevealed = !entry.item.isPrivate || entry.item.id in revealedProjectIds,
                        onClick = { onProjectClick(entry.item) },
                        onEdit = { editingProject = entry.item },
                        onDelete = { deletingProject = entry.item },
                        onRequestReveal = {
                            pendingRevealId = entry.item.id
                            onBeginReauth()
                            showAuthGate = true
                        },
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        ProjectFormSheet(
            existing = null,
            onDismiss = { showCreateSheet = false },
            onConfirm = { title, deadlineAt, isPrivate ->
                onCreateProject(title, deadlineAt, isPrivate)
                showCreateSheet = false
            },
        )
    }

    editingProject?.let { project ->
        ProjectFormSheet(
            existing = project,
            onDismiss = { editingProject = null },
            onConfirm = { title, deadlineAt, isPrivate ->
                onUpdateProject(project, title, deadlineAt, isPrivate)
                editingProject = null
            },
        )
    }

    deletingProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("Supprimer « ${project.title} » ?") },
            text = { Text("Tous ses objectifs et versements seront supprimés définitivement.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProject(project)
                    deletingProject = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingProject = null }) { Text("Annuler") } },
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
                pendingRevealId?.let(onRevealProject)
                pendingRevealId = null
            },
            onDismiss = {
                showAuthGate = false
                pendingRevealId = null
            },
        )
    }
}

@Composable
private fun ProjectCard(
    entry: ProjectListEntry,
    displayCurrency: Currency,
    isRevealed: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestReveal: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val aggregate = remember(entry.objectives, displayCurrency) {
        ProjectAggregator.aggregate(entry.objectives, displayCurrency)
    }

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
                    if (aggregate.hasObjectives && isRevealed) {
                        Text("${(aggregate.percent * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
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

            if (!isRevealed) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "••• • •••••••",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRequestReveal) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Afficher")
                    }
                }
                return@Column
            }

            entry.item.deadlineAt?.let { deadline ->
                Spacer(Modifier.height(2.dp))
                val daysRemaining = remember(deadline) { daysUntil(deadline) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Event,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(deadline)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatDaysRemaining(deadline),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            daysRemaining < 0 -> MaterialTheme.colorScheme.error
                            daysRemaining <= 3 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (aggregate.hasObjectives) {
                LinearProgressIndicator(
                    progress = { aggregate.percent },
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatMoney(aggregate.totalRaised, displayCurrency)} / " +
                        formatMoney(aggregate.totalTarget, displayCurrency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Aucun objectif pour l'instant",
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
