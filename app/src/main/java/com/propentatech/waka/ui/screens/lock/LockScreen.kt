package com.propentatech.waka.ui.screens.lock

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(
    projectTitle: String,
    uiState: LockUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometricRequested: (FragmentActivity) -> Unit,
    onUnlocked: () -> Unit,
) {
    val activity = LocalActivity.current as? FragmentActivity

    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) onUnlocked()
    }
    LaunchedEffect(activity) {
        activity?.let(onBiometricRequested)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(projectTitle, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Projet protégé",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        if (!uiState.pinConfigured) {
            Text(
                "Aucun code PIN configuré. Ouvre Paramètres pour en créer un.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        PinDots(length = uiState.pin.length)
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
        Keypad(enabled = !uiState.isLockedOut && uiState.pinConfigured, onDigit = onDigit, onBackspace = onBackspace)

        Spacer(Modifier.height(20.dp))
        if (activity != null) {
            TextButton(onClick = { onBiometricRequested(activity) }) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Utiliser l'empreinte")
            }
        }
    }
}

@Composable
private fun PinDots(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { index ->
            val filled = index < length
            Row(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {}
        }
    }
}

@Composable
private fun Keypad(enabled: Boolean, onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { digit ->
                    KeypadButton(digit.toString(), enabled) { onDigit(digit) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(72.dp))
            KeypadButton("0", enabled) { onDigit('0') }
            Surface(
                onClick = onBackspace,
                enabled = enabled,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(72.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Effacer")
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(72.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        }
    }
}
