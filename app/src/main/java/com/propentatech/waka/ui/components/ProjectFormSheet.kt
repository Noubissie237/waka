package com.propentatech.waka.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.propentatech.waka.data.local.entity.ProjectItem
import java.text.DateFormat
import java.util.Date

/** Un projet ne porte ni montant ni devise : seulement un nom, une échéance et sa confidentialité. */
@Composable
fun ProjectFormSheet(
    existing: ProjectItem?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, deadlineAt: Long?, isPrivate: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var deadlineAt by remember { mutableStateOf(existing?.deadlineAt) }
    var isPrivate by remember { mutableStateOf(existing?.isPrivate ?: false) }

    WakaFormSheet(
        title = if (existing == null) "Nouveau projet" else "Modifier le projet",
        subtitle = "Un nom, une échéance, le budget se règle objectif par objectif.",
        onDismiss = onDismiss,
        primaryLabel = if (existing == null) "Créer" else "Enregistrer",
        primaryEnabled = title.isNotBlank(),
        onPrimaryClick = { onConfirm(title, deadlineAt, isPrivate) },
    ) {
        WakaField(label = "Titre", value = title, onValueChange = { title = it }, placeholder = "Vayage Paris")
        Spacer(Modifier.height(18.dp))
        Text(
            "ÉCHÉANCE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = {
                showDatePicker(context, deadlineAt ?: System.currentTimeMillis()) { deadlineAt = it }
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    deadlineAt?.let { DateFormat.getDateInstance(DateFormat.LONG).format(Date(it)) }
                        ?: "Choisir une échéance (optionnel)",
                )
            }
        }
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
