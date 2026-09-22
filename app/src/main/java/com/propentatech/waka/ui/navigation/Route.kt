package com.propentatech.waka.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home : Route

    /** Écran de verrouillage pour le projet racine [projectItemId], avant d'accéder à son détail. */
    @Serializable
    data class Lock(val projectItemId: Long) : Route

    @Serializable
    data class ProjectDetail(val projectItemId: Long) : Route

    @Serializable
    data object Notes : Route

    /** [noteId] nul = création d'une nouvelle fiche. */
    @Serializable
    data class NoteDetail(val noteId: Long? = null) : Route

    @Serializable
    data object Settings : Route
}
