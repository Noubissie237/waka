package com.propentatech.waka

import android.content.Context
import com.propentatech.waka.data.local.WakaDatabase
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.data.repository.ReminderRepository
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.SecurityPreferences

/** Conteneur de dépendances manuel (pas de Hilt) : instanciation paresseuse, partagée pour toute la durée de vie de l'app. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    private val database by lazy { WakaDatabase.getInstance(context) }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectItemDao(), database.contributionDao(), database.taskDependencyDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    val appPreferences: AppPreferences by lazy { AppPreferences(context) }

    val securityPreferences: SecurityPreferences by lazy { SecurityPreferences(context) }

    val biometricAuthenticator: BiometricAuthenticator by lazy { BiometricAuthenticator() }
}
