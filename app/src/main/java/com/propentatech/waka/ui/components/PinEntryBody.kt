package com.propentatech.waka.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.propentatech.waka.security.PinAuthUiState

/** Icône, titre, pavé PIN et repli empreinte, partagé entre le verrouillage de projet et la ré-authentification. */
@Composable
fun PinEntryBody(
    title: String,
    subtitle: String,
    uiState: PinAuthUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    activity: FragmentActivity?,
    onBiometricRequested: (FragmentActivity) -> Unit,
) {
    Icon(
        Icons.Filled.Lock,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))

    if (!uiState.pinConfigured) {
        Text(
            "Aucun code PIN configuré. Ouvre Paramètres pour en créer un.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }

    WakaPinDots(length = uiState.pin.length)
    Spacer(Modifier.height(12.dp))

    when {
        uiState.isLockedOut -> Text(
            "Trop d'essais. Réessaie dans un instant.",
            color = MaterialTheme.colorScheme.error,
        )
        uiState.error != null -> Text(uiState.error, color = MaterialTheme.colorScheme.error)
        else -> Spacer(Modifier.height(20.dp))
    }

    Spacer(Modifier.height(20.dp))
    WakaPinKeypad(enabled = !uiState.isLockedOut && uiState.pinConfigured, onDigit = onDigit, onBackspace = onBackspace)

    Spacer(Modifier.height(20.dp))
    if (activity != null) {
        TextButton(onClick = { onBiometricRequested(activity) }) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Utiliser l'empreinte")
        }
    }
}
