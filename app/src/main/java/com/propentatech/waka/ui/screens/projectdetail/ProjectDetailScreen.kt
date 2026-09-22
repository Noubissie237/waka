package com.propentatech.waka.ui.screens.projectdetail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.domain.ProjectProgress
import com.propentatech.waka.domain.SavingsEstimator
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.convert
import com.propentatech.waka.ui.components.WakaField
import com.propentatech.waka.ui.components.WakaFormSheet
import com.propentatech.waka.ui.components.WakaSegmentedControl
import com.propentatech.waka.ui.format.formatMoney
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onAddContribution: (amount: Double, currency: Currency, dateMillis: Long, note: String?) -> Unit,
    onDeleteContribution: (Contribution) -> Unit,
    onToggleManualCompletion: (ProjectItem) -> Unit,
    onCreateChild: (title: String, targetAmount: Double?, currency: Currency?, prerequisiteIds: Set<Long>) -> Unit,
    onUpdateChild: (ProjectItem, title: String, targetAmount: Double?, currency: Currency?) -> Unit,
    onDeleteChild: (ProjectItem) -> Unit,
    onUpdateSelf: (title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) -> Unit,
    onDeleteSelf: () -> Unit,
    onChildClick: (ProjectItem) -> Unit,
) {
    var showAddContribution by remember { mutableStateOf(false) }
    var showCreateChild by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ProjectItem?>(null) }
    var deletingChild by remember { mutableStateOf<ProjectItem?>(null) }
    var showEditSelf by remember { mutableStateOf(false) }
    var showDeleteSelf by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, uiState.item) {
        if (!uiState.isLoading && uiState.item == null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.title ?: "…") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (uiState.item != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Modifier") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = { showMenu = false; showEditSelf = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Supprimer") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = { showMenu = false; showDeleteSelf = true },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.item != null) {
                FloatingActionButton(onClick = { showCreateChild = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter une tâche")
                }
            }
        },
    ) { innerPadding ->
        val item = uiState.item
        if (uiState.isLoading || item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.targetAmount != null && item.currency != null && uiState.ownProgress != null) {
                item {
                    BudgetSection(
                        currency = item.currency,
                        displayCurrency = uiState.displayCurrency,
                        progress = uiState.ownProgress,
                        estimate = uiState.estimate,
                        onAddContribution = { showAddContribution = true },
                    )
                }
                if (uiState.contributions.isNotEmpty()) {
                    item {
                        Text("Historique des versements", style = MaterialTheme.typography.titleSmall)
                    }
                    items(uiState.contributions, key = { it.id }) { contribution ->
                        ContributionRow(contribution, onDelete = { onDeleteContribution(contribution) })
                    }
                }
            }

            if (uiState.children.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Tâches", style = MaterialTheme.typography.titleSmall)
                }
                items(uiState.children, key = { it.item.id }) { entry ->
                    ChildRow(
                        entry = entry,
                        onClick = { onChildClick(entry.item) },
                        onToggleManual = { onToggleManualCompletion(entry.item) },
                        onEdit = { editingChild = entry.item },
                        onDelete = { deletingChild = entry.item },
                    )
                }
            } else if (item.targetAmount == null) {
                item {
                    Text(
                        "Aucune tâche pour l'instant. Touchez + pour en ajouter une.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showAddContribution && uiState.item?.currency != null) {
        AddContributionSheet(
            defaultCurrency = uiState.item.currency,
            onDismiss = { showAddContribution = false },
            onConfirm = { amount, currency, note ->
                onAddContribution(amount, currency, System.currentTimeMillis(), note)
                showAddContribution = false
            },
        )
    }

    if (showCreateChild) {
        ItemFormSheet(
            titleText = "Nouvelle tâche",
            existing = null,
            showPrivacyToggle = false,
            siblings = uiState.children.map { it.item },
            onDismiss = { showCreateChild = false },
            onConfirm = { title, amount, currency, _, prerequisiteIds ->
                onCreateChild(title, amount, currency, prerequisiteIds)
                showCreateChild = false
            },
        )
    }

    editingChild?.let { child ->
        ItemFormSheet(
            titleText = "Modifier la tâche",
            existing = child,
            showPrivacyToggle = false,
            siblings = emptyList(),
            onDismiss = { editingChild = null },
            onConfirm = { title, amount, currency, _, _ ->
                onUpdateChild(child, title, amount, currency)
                editingChild = null
            },
        )
    }

    if (showEditSelf && uiState.item != null) {
        ItemFormSheet(
            titleText = "Modifier le projet",
            existing = uiState.item,
            showPrivacyToggle = uiState.item.parentId == null,
            siblings = emptyList(),
            onDismiss = { showEditSelf = false },
            onConfirm = { title, amount, currency, isPrivate, _ ->
                onUpdateSelf(title, amount, currency, isPrivate)
                showEditSelf = false
            },
        )
    }

    deletingChild?.let { child ->
        AlertDialog(
            onDismissRequest = { deletingChild = null },
            title = { Text("Supprimer « ${child.title} » ?") },
            text = { Text("Ses éventuelles sous-tâches et versements seront supprimés définitivement.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteChild(child)
                    deletingChild = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingChild = null }) { Text("Annuler") } },
        )
    }

    if (showDeleteSelf && uiState.item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSelf = false },
            title = { Text("Supprimer « ${uiState.item.title} » ?") },
            text = { Text("Toutes ses tâches et versements seront supprimés définitivement.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSelf()
                    showDeleteSelf = false
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteSelf = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun BudgetSection(
    currency: Currency,
    displayCurrency: Currency,
    progress: ProjectProgress,
    estimate: SavingsEstimator.Estimate?,
    onAddContribution: () -> Unit,
) {
    val target = progress.targetAmount ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                formatMoney(progress.raised, currency),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (displayCurrency != currency) {
                Text(
                    formatMoney(progress.raised.convert(currency, displayCurrency), displayCurrency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "sur ${formatMoney(target, currency)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress.percent }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            if (progress.isCompleted) {
                Text("Objectif atteint", color = MaterialTheme.colorScheme.primary)
            } else {
                progress.remaining?.let { Text("Reste à trouver : ${formatMoney(it, currency)}") }
                estimate?.let {
                    Spacer(Modifier.height(4.dp))
                    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it.estimatedDateMillis))
                    Text(
                        "À ce rythme (~${formatMoney(it.monthlyAverage, currency)}/mois en moyenne), " +
                            "objectif atteint ≈ $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAddContribution) { Text("+ Ajouter un versement") }
        }
    }
}

@Composable
private fun ContributionRow(contribution: Contribution, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(formatMoney(contribution.amount, contribution.currency))
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(contribution.date)) +
                    (contribution.note?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Supprimer le versement")
        }
    }
    HorizontalDivider()
}

@Composable
private fun ChildRow(
    entry: ChildEntry,
    onClick: () -> Unit,
    onToggleManual: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val currency = entry.item.currency
    val budgeted = entry.item.targetAmount != null && currency != null
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!entry.isLocked && budgeted) onClick() },
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                entry.isLocked -> Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Verrouillé",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                !budgeted -> Checkbox(checked = entry.progress.isCompleted, onCheckedChange = { onToggleManual() })
                entry.progress.isCompleted -> Icon(
                    Icons.Filled.Check,
                    contentDescription = "Terminé",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                else -> Spacer(Modifier.size(20.dp))
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (entry.progress.isCompleted) TextDecoration.LineThrough else null,
                )
                if (currency != null && entry.progress.targetAmount != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { entry.progress.percent },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${formatMoney(entry.progress.raised, currency)} / " +
                            formatMoney(entry.progress.targetAmount, currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (entry.isLocked) {
                    Text(
                        "Verrouillé — prérequis non complété",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
}

@Composable
private fun AddContributionSheet(
    defaultCurrency: Currency,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, currency: Currency, note: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var note by remember { mutableStateOf("") }

    WakaFormSheet(
        title = "Ajouter un versement",
        subtitle = "Le montant que tu viens de mettre de côté.",
        onDismiss = onDismiss,
        primaryLabel = "Enregistrer",
        primaryEnabled = amountText.toDoubleOrNull() != null,
        onPrimaryClick = { amountText.toDoubleOrNull()?.let { onConfirm(it, currency, note) } },
    ) {
        WakaField(
            label = "Montant",
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
        Spacer(Modifier.height(18.dp))
        WakaField(
            label = "Note (optionnel)",
            value = note,
            onValueChange = { note = it },
            placeholder = "Acompte studio…",
        )
    }
}

@Composable
private fun ItemFormSheet(
    titleText: String,
    existing: ProjectItem?,
    showPrivacyToggle: Boolean,
    siblings: List<ProjectItem>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean, prerequisiteIds: Set<Long>) -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember {
        mutableStateOf(
            existing?.targetAmount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }.orEmpty(),
        )
    }
    var currency by remember { mutableStateOf(existing?.currency ?: Currency.EUR) }
    var isPrivate by remember { mutableStateOf(existing?.isPrivate ?: false) }
    var selectedPrerequisites by remember { mutableStateOf(setOf<Long>()) }

    WakaFormSheet(
        title = titleText,
        onDismiss = onDismiss,
        primaryLabel = if (existing == null) "Créer" else "Enregistrer",
        primaryEnabled = title.isNotBlank(),
        onPrimaryClick = {
            val amount = amountText.toDoubleOrNull()
            onConfirm(title, amount, if (amount != null) currency else null, isPrivate, selectedPrerequisites)
        },
    ) {
        WakaField(label = "Titre", value = title, onValueChange = { title = it }, placeholder = "Studio meublé")
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
        if (showPrivacyToggle) {
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Projet privé", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
            }
        }
        if (siblings.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Text(
                "DÉPEND DE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            siblings.forEach { sibling ->
                val checked = sibling.id in selectedPrerequisites
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selectedPrerequisites = if (isChecked) {
                                selectedPrerequisites + sibling.id
                            } else {
                                selectedPrerequisites - sibling.id
                            }
                        },
                    )
                    Text(sibling.title)
                }
            }
        }
    }
}
