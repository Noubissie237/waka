package com.propentatech.waka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GreenLight,
    onPrimary = OnGreenLight,
    primaryContainer = GreenContainerLight,
    onPrimaryContainer = OnGreenContainerLight,
    secondary = GoldLight,
    onSecondary = OnGoldLight,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = OnGoldContainerLight,
    tertiary = NavyLight,
    onTertiary = OnNavyLight,
    error = RedLight,
    onError = OnRedLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenDark,
    onPrimary = OnGreenDark,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = OnGreenContainerDark,
    secondary = GoldDark,
    onSecondary = OnGoldDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = OnGoldContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    error = RedDark,
    onError = OnRedDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

/**
 * Palette dérivée du logo, volontairement fixe : pas de couleur dynamique (Material You),
 * pour garder une identité stable indépendante du fond d'écran de l'utilisateur.
 */
@Composable
fun WakaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
