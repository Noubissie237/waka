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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.domain.ProjectProgress
import com.propentatech.waka.domain.SavingsEstimator
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.model.convert
import com.propentatech.waka.ui.components.ProjectFormSheet
import com.propentatech.waka.ui.components.ReminderFormSheet
import com.propentatech.waka.ui.components.WakaField
import com.propentatech.waka.ui.components.WakaFormSheet
import com.propentatech.waka.ui.components.WakaTopBar
import com.propentatech.waka.ui.components.WakaSegmentedControl
import com.propentatech.waka.ui.format.formatMoney
import kotlinx.coroutines.flow.Flow
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    uiState: ProjectDetailUiState,
    contributionEvents: Flow<ContributionEvent>,
    onBack: () -> Unit,
    onAddContribution: (amount: Double, currency: Currency, dateMillis: Long, note: String?) -> Unit,
    onDeleteContribution: (Contribution) -> Unit,
    onToggleManualCompletion: (ProjectItem) -> Unit,
    onCreateChild: (title: String, targetAmount: Double?, currency: Currency?, prerequisiteIds: Set<Long>) -> Unit,
    onUpdateChild: (ProjectItem, title: String, targetAmount: Double?, currency: Currency?) -> Unit,
    onDeleteChild: (ProjectItem) -> Unit,
    onUpdateSelfAsProject: (title: String, deadlineAt: Long?, isPrivate: Boolean) -> Unit,
    onUpdateSelfAsObjective: (title: String, targetAmount: Double?, currency: Currency?) -> Unit,
    onDeleteSelf: () -> Unit,
    onChildClick: (ProjectItem) -> Unit,
    onAddReminder: (hour: Int, minute: Int, weekday: Int?, repeatType: RepeatType, message: String) -> Unit,
    onUpdateReminder: (Reminder, hour: Int, minute: Int, weekday: Int?, repeatType: RepeatType, message: String) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
) {
    var showAddContribution by remember { mutableStateOf(false) }
    var showCreateChild by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ProjectItem?>(null) }
    var deletingChild by remember { mutableStateOf<ProjectItem?>(null) }
    var showEditSelf by remember { mutableStateOf(false) }
    var showDeleteSelf by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var deletingReminder by remember { mutableStateOf<Reminder?>(null) }
    var contributionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(contributionEvents) {
        contributionEvents.collect { event ->
            when (event) {
                is ContributionEvent.Success -> {
                    showAddContribution = false
                    contributionError = null
                }
                is ContributionEvent.Rejected -> {
                    contributionError = "Ce montant dépasse ce qu'il reste à financer sur ce projet " +
                        "(il reste ${formatMoney(event.totalRemaining, event.currency)}). " +
                        "Réduis le montant ou ajoute le reste sur un autre projet."
                }
            }
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.item) {
        if (!uiState.isLoading && uiState.item == null) onBack()
    }

    Scaffold(
        topBar = {
            WakaTopBar(
                title = uiState.item?.title ?: "…",
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
                val isRoot = uiState.item.parentId == null
                FloatingActionButton(onClick = { showCreateChild = true }) {
                    Icon(Icons.Filled.Add, contentDescription = if (isRoot) "Ajouter un objectif" else "Ajouter une tâche")
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

            val isRoot = item.parentId == null
            if (uiState.children.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(if (isRoot) "Objectifs" else "Tâches", style = MaterialTheme.typography.titleSmall)
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
                        if (isRoot) {
                            "Aucun objectif pour l'instant. Touchez + pour en ajouter un."
                        } else {
                            "Aucune tâche pour l'instant. Touchez + pour en ajouter une."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rappels", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { showAddReminder = true }) { Text("+ Ajouter") }
                }
            }
            if (uiState.reminders.isEmpty()) {
                item {
                    Text(
                        "Aucun rappel programmé pour ${if (isRoot) "ce projet" else "cet objectif"}.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder,
                        onEdit = { editingReminder = reminder },
                        onDelete = { deletingReminder = reminder },
                    )
                }
            }
        }
    }

    if (showAddContribution && uiState.item?.currency != null) {
        AddContributionSheet(
            defaultCurrency = uiState.item.currency,
            errorMessage = contributionError,
            onDismiss = { showAddContribution = false; contributionError = null },
            onConfirm = { amount, currency, note ->
                contributionError = null
                onAddContribution(amount, currency, System.currentTimeMillis(), note)
            },
        )
    }

    val isCurrentRoot = uiState.item?.parentId == null

    if (showCreateChild) {
        ItemFormSheet(
            titleText = if (isCurrentRoot) "Nouvel objectif" else "Nouvelle tâche",
            amountRequired = isCurrentRoot,
            existing = null,
            siblings = uiState.children.map { it.item },
            onDismiss = { showCreateChild = false },
            onConfirm = { title, amount, currency, prerequisiteIds ->
                onCreateChild(title, amount, currency, prerequisiteIds)
                showCreateChild = false
            },
        )
    }

    editingChild?.let { child ->
        ItemFormSheet(
            titleText = if (isCurrentRoot) "Modifier l'objectif" else "Modifier la tâche",
            amountRequired = isCurrentRoot,
            existing = child,
            siblings = emptyList(),
            onDismiss = { editingChild = null },
            onConfirm = { title, amount, currency, _ ->
                onUpdateChild(child, title, amount, currency)
                editingChild = null
            },
        )
    }

    if (showEditSelf && uiState.item != null) {
        if (isCurrentRoot) {
            ProjectFormSheet(
                existing = uiState.item,
                onDismiss = { showEditSelf = false },
                onConfirm = { title, deadlineAt, isPrivate ->
                    onUpdateSelfAsProject(title, deadlineAt, isPrivate)
                    showEditSelf = false
                },
            )
        } else {
            ItemFormSheet(
                titleText = "Modifier la tâche",
                amountRequired = false,
                existing = uiState.item,
                siblings = emptyList(),
                onDismiss = { showEditSelf = false },
                onConfirm = { title, amount, currency, _ ->
                    onUpdateSelfAsObjective(title, amount, currency)
                    showEditSelf = false
                },
            )
        }
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

    if (showAddReminder) {
        ReminderFormSheet(
            existing = null,
            onDismiss = { showAddReminder = false },
            onConfirm = { hour, minute, weekday, repeatType, message ->
                onAddReminder(hour, minute, weekday, repeatType, message)
                showAddReminder = false
            },
        )
    }

    editingReminder?.let { reminder ->
        ReminderFormSheet(
            existing = reminder,
            onDismiss = { editingReminder = null },
            onConfirm = { hour, minute, weekday, repeatType, message ->
                onUpdateReminder(reminder, hour, minute, weekday, repeatType, message)
                editingReminder = null
            },
        )
    }

    deletingReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { deletingReminder = null },
            title = { Text("Supprimer ce rappel ?") },
            text = { Text("« ${reminder.message} » ne se déclenchera plus.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteReminder(reminder)
                    deletingReminder = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingReminder = null }) { Text("Annuler") } },
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
                    (contribution.note?.let { ", $it" } ?: ""),
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
private fun ReminderRow(reminder: Reminder, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(reminder.message)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(reminder.triggerAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (reminder.repeatType != RepeatType.NONE) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "Récurrent",
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        modifier = Modifier.fillMaxWidth().alpha(if (entry.isLocked) 0.5f else 1f),
        colors = if (entry.isLocked) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        } else {
            CardDefaults.cardColors()
        },
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
                        "Verrouillé, prérequis non complété",
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
    errorMessage: String?,
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
        if (errorMessage != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ItemFormSheet(
    titleText: String,
    amountRequired: Boolean,
    existing: ProjectItem?,
    siblings: List<ProjectItem>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double?, currency: Currency?, prerequisiteIds: Set<Long>) -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember {
        mutableStateOf(
            existing?.targetAmount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }.orEmpty(),
        )
    }
    var currency by remember { mutableStateOf(existing?.currency ?: Currency.EUR) }
    var selectedPrerequisites by remember { mutableStateOf(setOf<Long>()) }
    val amount = amountText.toDoubleOrNull()

    WakaFormSheet(
        title = titleText,
        onDismiss = onDismiss,
        primaryLabel = if (existing == null) "Créer" else "Enregistrer",
        primaryEnabled = title.isNotBlank() && (!amountRequired || amount != null),
        onPrimaryClick = {
            onConfirm(title, amount, if (amount != null) currency else null, selectedPrerequisites)
        },
    ) {
        WakaField(label = "Titre", value = title, onValueChange = { title = it }, placeholder = "Acheter une valise")
        Spacer(Modifier.height(18.dp))
        WakaField(
            label = if (amountRequired) "Montant" else "Montant cible (optionnel)",
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
