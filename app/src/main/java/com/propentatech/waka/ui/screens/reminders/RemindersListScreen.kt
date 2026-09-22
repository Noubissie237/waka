package com.propentatech.waka.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.ui.components.ReminderFormSheet
import com.propentatech.waka.ui.components.WakaTopBar
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersListScreen(
    reminders: List<ReminderWithContext>,
    onUpdate: (Reminder, hour: Int, minute: Int, weekday: Int?, repeatType: RepeatType, message: String) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var deletingReminder by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(topBar = { WakaTopBar(title = "Rappels") }) { innerPadding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aucun rappel programmé. Ouvre un projet ou un objectif pour en créer un.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(reminders, key = { it.reminder.id }) { entry ->
                    ReminderRow(
                        entry,
                        onEdit = { editingReminder = entry.reminder },
                        onDelete = { deletingReminder = entry.reminder },
                    )
                }
            }
        }
    }

    editingReminder?.let { reminder ->
        ReminderFormSheet(
            existing = reminder,
            onDismiss = { editingReminder = null },
            onConfirm = { hour, minute, weekday, repeatType, message ->
                onUpdate(reminder, hour, minute, weekday, repeatType, message)
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
                    onDelete(reminder)
                    deletingReminder = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingReminder = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun ReminderRow(entry: ReminderWithContext, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(entry.reminder.message, style = MaterialTheme.typography.bodyLarge)
                Text(
                    entry.parentTitle?.let { "$it · ${entry.itemTitle}" } ?: entry.itemTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(entry.reminder.triggerAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (entry.reminder.repeatType != RepeatType.NONE) {
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
    }
}
