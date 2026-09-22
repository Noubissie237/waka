package com.propentatech.waka

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.propentatech.waka.ui.LocalAppContainer
import com.propentatech.waka.ui.navigation.Route
import com.propentatech.waka.ui.screens.home.HomeScreen
import com.propentatech.waka.ui.screens.home.HomeViewModel
import com.propentatech.waka.ui.screens.lock.LockScreen
import com.propentatech.waka.ui.screens.lock.LockViewModel
import com.propentatech.waka.ui.screens.notes.NoteEditScreen
import com.propentatech.waka.ui.screens.notes.NoteEditViewModel
import com.propentatech.waka.ui.screens.notes.NotesListScreen
import com.propentatech.waka.ui.screens.notes.NotesViewModel
import com.propentatech.waka.ui.screens.projectdetail.ProjectDetailScreen
import com.propentatech.waka.ui.screens.projectdetail.ProjectDetailViewModel
import com.propentatech.waka.ui.screens.settings.SettingsScreen
import com.propentatech.waka.ui.screens.settings.SettingsViewModel
import com.propentatech.waka.ui.theme.WakaTheme

/**
 * BiometricPrompt exige une FragmentActivity (pas la ComponentActivity du template Compose par défaut).
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WakaApplication).container
        setContent {
            WakaTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    WakaApp(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun WakaApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isTopLevel = destination?.hasRoute<Route.Home>() == true ||
        destination?.hasRoute<Route.Notes>() == true ||
        destination?.hasRoute<Route.Settings>() == true

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {}
        LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute<Route.Home>() == true,
                        onClick = { navController.navigate(Route.Home) { popUpTo(Route.Home) { inclusive = true } } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Accueil") },
                    )
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute<Route.Notes>() == true,
                        onClick = {
                            navController.navigate(Route.Notes) { popUpTo(Route.Home) }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        label = { Text("Notes") },
                    )
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute<Route.Settings>() == true,
                        onClick = {
                            navController.navigate(Route.Settings) { popUpTo(Route.Home) }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Réglages") },
                    )
                }
            }
        },
    ) { innerPadding ->
        WakaNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun WakaNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current

    NavHost(navController = navController, startDestination = Route.Home, modifier = modifier) {
        composable<Route.Home> {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HomeViewModel(container.projectRepository, container.appPreferences) }
                },
            )
            val uiState by viewModel.uiState.collectAsState()
            HomeScreen(
                uiState = uiState,
                onDisplayCurrencyChange = viewModel::setDisplayCurrency,
                onCreateProject = viewModel::createRootProject,
                onUpdateProject = viewModel::updateProject,
                onDeleteProject = viewModel::deleteProject,
                onProjectClick = { item ->
                    if (item.isPrivate) {
                        navController.navigate(Route.Lock(item.id))
                    } else {
                        navController.navigate(Route.ProjectDetail(item.id))
                    }
                },
            )
        }

        composable<Route.Lock> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Lock>()
            val viewModel: LockViewModel = viewModel(
                key = "lock_${route.projectItemId}",
                factory = viewModelFactory {
                    initializer { LockViewModel(container.securityPreferences, container.biometricAuthenticator) }
                },
            )
            val uiState by viewModel.uiState.collectAsState()
            val projectTitle by produceState(initialValue = "") {
                container.projectRepository.observeItem(route.projectItemId).collect { value = it?.title ?: "" }
            }
            LockScreen(
                projectTitle = projectTitle,
                uiState = uiState,
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                onBiometricRequested = viewModel::tryBiometric,
                onUnlocked = {
                    navController.navigate(Route.ProjectDetail(route.projectItemId)) {
                        popUpTo(Route.Lock(route.projectItemId)) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.ProjectDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.ProjectDetail>()
            val viewModel: ProjectDetailViewModel = viewModel(
                key = "project_detail_${route.projectItemId}",
                factory = viewModelFactory {
                    initializer {
                        ProjectDetailViewModel(
                            container.projectRepository,
                            container.appPreferences,
                            route.projectItemId,
                        )
                    }
                },
            )
            val uiState by viewModel.uiState.collectAsState()
            ProjectDetailScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onAddContribution = viewModel::addContribution,
                onDeleteContribution = viewModel::deleteContribution,
                onToggleManualCompletion = viewModel::toggleManualCompletion,
                onCreateChild = viewModel::createChild,
                onUpdateChild = viewModel::updateChild,
                onDeleteChild = viewModel::deleteChild,
                onUpdateSelf = viewModel::updateSelf,
                onDeleteSelf = viewModel::deleteSelf,
                onChildClick = { child -> navController.navigate(Route.ProjectDetail(child.id)) },
            )
        }

        composable<Route.Notes> {
            val viewModel: NotesViewModel = viewModel(
                factory = viewModelFactory { initializer { NotesViewModel(container.noteRepository) } },
            )
            val notes by viewModel.notes.collectAsState()
            NotesListScreen(
                notes = notes,
                onNoteClick = { id -> navController.navigate(Route.NoteDetail(id)) },
                onCreateNote = { navController.navigate(Route.NoteDetail(null)) },
            )
        }

        composable<Route.NoteDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.NoteDetail>()
            val viewModel: NoteEditViewModel = viewModel(
                key = "note_${route.noteId}",
                factory = viewModelFactory {
                    initializer { NoteEditViewModel(container.noteRepository, container.appContext, route.noteId) }
                },
            )
            val uiState by viewModel.uiState.collectAsState()
            NoteEditScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSave = { title, content -> viewModel.save(title, content) {} },
                onDelete = viewModel::delete,
                onAddReminder = viewModel::addReminder,
                onDeleteReminder = viewModel::deleteReminder,
            )
        }

        composable<Route.Settings> {
            val viewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { SettingsViewModel(container.appPreferences, container.securityPreferences) }
                },
            )
            val displayCurrency by viewModel.displayCurrency.collectAsState()
            val isPinSet by viewModel.isPinSet.collectAsState()
            val biometricEnabled by viewModel.biometricEnabled.collectAsState()
            SettingsScreen(
                displayCurrency = displayCurrency,
                isPinSet = isPinSet,
                biometricEnabled = biometricEnabled,
                biometricAuthenticator = container.biometricAuthenticator,
                onDisplayCurrencyChange = viewModel::setDisplayCurrency,
                onSetPin = viewModel::setPin,
                onClearPin = viewModel::clearPin,
                onBiometricEnabledChange = viewModel::setBiometricEnabled,
            )
        }
    }
}
