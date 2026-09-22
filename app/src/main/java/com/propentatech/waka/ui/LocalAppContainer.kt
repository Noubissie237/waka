package com.propentatech.waka.ui

import androidx.compose.runtime.compositionLocalOf
import com.propentatech.waka.AppContainer

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer non fourni : enveloppe le contenu dans CompositionLocalProvider(LocalAppContainer provides ...)")
}
