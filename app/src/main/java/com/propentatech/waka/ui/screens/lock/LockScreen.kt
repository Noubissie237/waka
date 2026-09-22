package com.propentatech.waka.ui.screens.lock

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.ui.components.PinEntryBody

@Composable
fun LockScreen(
    projectTitle: String,
    uiState: PinAuthUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometricRequested: (FragmentActivity) -> Unit,
    onUnlocked: () -> Unit,
) {
    val activity = LocalActivity.current as? FragmentActivity

    LaunchedEffect(uiState.authenticated) {
        if (uiState.authenticated) onUnlocked()
    }
    LaunchedEffect(activity) {
        activity?.let(onBiometricRequested)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PinEntryBody(
            title = projectTitle,
            subtitle = "Projet protégé",
            uiState = uiState,
            onDigit = onDigit,
            onBackspace = onBackspace,
            activity = activity,
            onBiometricRequested = onBiometricRequested,
        )
    }
}
