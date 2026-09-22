package com.propentatech.waka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.model.RepeatType
import java.util.Calendar
import java.util.Locale

private val weekdays = listOf(
    Calendar.MONDAY to "Lu",
    Calendar.TUESDAY to "Ma",
    Calendar.WEDNESDAY to "Me",
    Calendar.THURSDAY to "Je",
    Calendar.FRIDAY to "Ve",
    Calendar.SATURDAY to "Sa",
    Calendar.SUNDAY to "Di",
)

/**
 * Aucune date à choisir : « Une fois » et « Jour » ne demandent qu'une heure (une fois = aujourd'hui
 * même, à une heure encore à venir ; jour = chaque jour). « Semaine » et « Mois » demandent en plus
 * un jour de la semaine. Utilisé à la fois pour créer et pour modifier un rappel.
 */
@Composable
fun ReminderFormSheet(
    existing: Reminder?,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, weekday: Int?, repeatType: RepeatType, message: String) -> Unit,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf(existing?.message.orEmpty()) }
    var repeatType by remember { mutableStateOf(existing?.repeatType ?: RepeatType.NONE) }

    val existingCalendar = remember(existing) {
        existing?.let { Calendar.getInstance().apply { timeInMillis = it.triggerAt } }
    }
    var pickedTime by remember {
        mutableStateOf(
            existingCalendar?.let { it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE) },
        )
    }
    var pickedWeekday by remember {
        mutableStateOf(existingCalendar?.get(Calendar.DAY_OF_WEEK))
    }

    // "Une fois" = aujourd'hui même, donc l'heure choisie doit être encore à venir.
    // "Jour" se répète chaque jour, peu importe le jour de départ : pas de jour de semaine à choisir.
    // Seuls "Semaine" et "Mois" ont besoin d'un jour de semaine.
    val needsWeekday = repeatType == RepeatType.WEEKLY || repeatType == RepeatType.MONTHLY
    val isPastForToday = repeatType == RepeatType.NONE && pickedTime?.let { (hour, minute) ->
        val now = Calendar.getInstance()
        hour < now.get(Calendar.HOUR_OF_DAY) ||
            (hour == now.get(Calendar.HOUR_OF_DAY) && minute <= now.get(Calendar.MINUTE))
    } == true

    WakaFormSheet(
        title = if (existing == null) "Nouveau rappel" else "Modifier le rappel",
        subtitle = "Une alarme système, même app fermée.",
        onDismiss = onDismiss,
        primaryLabel = if (existing == null) "Programmer" else "Enregistrer",
        primaryEnabled = pickedTime != null && !isPastForToday && (!needsWeekday || pickedWeekday != null),
        onPrimaryClick = {
            pickedTime?.let { (hour, minute) ->
                onConfirm(hour, minute, if (needsWeekday) pickedWeekday else null, repeatType, message)
            }
        },
    ) {
        WakaField(label = "Message", value = message, onValueChange = { message = it }, placeholder = "Rappel")
        Spacer(Modifier.height(18.dp))
        Text(
            "HEURE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = {
                val (h, m) = pickedTime ?: (9 to 0)
                showTimePicker(context, h, m) { hour, minute -> pickedTime = hour to minute }
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    pickedTime?.let { (h, m) -> String.format(Locale.getDefault(), "%02d:%02d", h, m) }
                        ?: "Choisir l'heure",
                )
            }
        }
        if (isPastForToday) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Pour un rappel unique, choisis une heure encore à venir aujourd'hui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
            label = {
                when (it) {
                    RepeatType.NONE -> "Une fois"
                    RepeatType.DAILY -> "Jour"
                    RepeatType.WEEKLY -> "Semaine"
                    RepeatType.MONTHLY -> "Mois"
                }
            },
        )
        if (needsWeekday) {
            Spacer(Modifier.height(18.dp))
            Text(
                "JOUR DE LA SEMAINE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekdays.forEach { (value, label) ->
                    val selected = pickedWeekday == value
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            )
                            .clickable { pickedWeekday = value },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}
