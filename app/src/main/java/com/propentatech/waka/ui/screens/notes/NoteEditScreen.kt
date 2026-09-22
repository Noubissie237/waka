package com.propentatech.waka.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.ui.components.WakaField
import com.propentatech.waka.ui.components.WakaFormSheet
import com.propentatech.waka.ui.components.WakaSegmentedControl
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    uiState: NoteEditUiState,
    onBack: () -> Unit,
    onSave: (title: String, content: String) -> Unit,
    onDelete: () -> Unit,
    onAddReminder: (triggerAtMillis: Long, message: String, repeatType: RepeatType) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
) {
    var title by remember(uiState.note?.id) { mutableStateOf(uiState.note?.title.orEmpty()) }
    var content by remember(uiState.note?.id) { mutableStateOf(uiState.note?.content.orEmpty()) }
    var showAddReminder by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.note?.id) {
        if (uiState.note != null) {
            title = uiState.note.title
            content = uiState.note.content
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "Nouvelle fiche" else "Fiche") },
                navigationIcon = {
                    IconButton(onClick = {
                        onSave(title, content)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = {
                            onDelete()
                            onBack()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer la fiche")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WakaField(label = "Titre", value = title, onValueChange = { title = it }, placeholder = "Sans titre")
            }
            item {
                WakaField(
                    label = "Contenu",
                    value = content,
                    onValueChange = { content = it },
                    placeholder = "Écris ici…",
                    singleLine = false,
                    minLines = 8,
                )
            }
            if (!uiState.isNew) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Rappels", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showAddReminder = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Text("Ajouter")
                        }
                    }
                }
                if (uiState.reminders.isEmpty()) {
                    item {
                        Text(
                            "Aucun rappel programmé.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(uiState.reminders, key = { it.id }) { reminder ->
                        ReminderRow(reminder, onDelete = { onDeleteReminder(reminder) })
                    }
                }
            }
        }
    }

    if (showAddReminder) {
        AddReminderDialog(
            onDismiss = { showAddReminder = false },
            onConfirm = { millis, message, repeatType ->
                onAddReminder(millis, message, repeatType)
                showAddReminder = false
            },
        )
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onDelete: () -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(reminder.message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(reminder.triggerAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer le rappel")
            }
        }
    }
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (triggerAtMillis: Long, message: String, repeatType: RepeatType) -> Unit,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var pickedMillis by remember { mutableStateOf<Long?>(null) }

    WakaFormSheet(
        title = "Nouveau rappel",
        subtitle = "Une alarme système, même app fermée.",
        onDismiss = onDismiss,
        primaryLabel = "Programmer",
        primaryEnabled = pickedMillis != null,
        onPrimaryClick = { pickedMillis?.let { onConfirm(it, message.ifBlank { "Rappel" }, repeatType) } },
    ) {
        WakaField(label = "Message", value = message, onValueChange = { message = it }, placeholder = "Rappel")
        Spacer(Modifier.height(18.dp))
        Text(
            "DATE ET HEURE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = { showDateTimePicker(context, System.currentTimeMillis()) { pickedMillis = it } },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    pickedMillis?.let {
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
                    } ?: "Choisir la date et l'heure",
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "RÉPÉTITION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        WakaSegmentedControl(
            options = RepeatType.entries,
            selected = repeatType,
            onSelect = { repeatType = it },
            label = ::repeatLabel,
        )
    }
}

private fun repeatLabel(type: RepeatType): String = when (type) {
    RepeatType.NONE -> "Une fois"
    RepeatType.DAILY -> "Chaque jour"
    RepeatType.WEEKLY -> "Chaque semaine"
    RepeatType.MONTHLY -> "Chaque mois"
}
