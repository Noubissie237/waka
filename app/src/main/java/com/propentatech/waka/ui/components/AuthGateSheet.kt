package com.propentatech.waka.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.propentatech.waka.security.PinAuthUiState

/**
 * Feuille de ré-authentification (PIN ou empreinte) exigée avant une action sensible des
 * Paramètres : modifier/supprimer le code PIN, ou (dés)activer le déverrouillage biométrique.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthGateSheet(
    uiState: PinAuthUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometricRequested: (FragmentActivity) -> Unit,
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activity = LocalActivity.current as? FragmentActivity
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.authenticated) {
        if (uiState.authenticated) onAuthenticated()
    }
    LaunchedEffect(activity) {
        activity?.let(onBiometricRequested)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.outline),
            )
            Spacer(Modifier.height(22.dp))
            PinEntryBody(
                title = "Confirmer ton identité",
                subtitle = "Nécessaire pour cette action sensible.",
                uiState = uiState,
                onDigit = onDigit,
                onBackspace = onBackspace,
                activity = activity,
                onBiometricRequested = onBiometricRequested,
            )
        }
    }
}
