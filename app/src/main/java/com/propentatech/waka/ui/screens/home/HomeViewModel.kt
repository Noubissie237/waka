package com.propentatech.waka.ui.screens.home

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.domain.ProjectProgress
import com.propentatech.waka.domain.ProjectProgressCalculator
import com.propentatech.waka.model.Currency
import com.propentatech.waka.security.BiometricAuthenticator
import com.propentatech.waka.security.PinAuthController
import com.propentatech.waka.security.PinAuthUiState
import com.propentatech.waka.security.SecurityPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un projet + ses objectifs directs, bruts (non convertis), la conversion vers la devise
 * d'affichage se fait à l'écran via [com.propentatech.waka.domain.ProjectAggregator]. */
data class ProjectListEntry(val item: ProjectItem, val objectives: List<Pair<ProjectItem, ProjectProgress>>)

data class HomeUiState(
    val projects: List<ProjectListEntry> = emptyList(),
    val displayCurrency: Currency = Currency.EUR,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val projectRepository: ProjectRepository,
    private val appPreferences: AppPreferences,
    securityPreferences: SecurityPreferences,
    biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    /** Les projets privés s'affichent masqués sur l'accueil tant qu'on ne les a pas révélés un par un. */
    private val reauthController = PinAuthController(
        scope = viewModelScope,
        securityPreferences = securityPreferences,
        biometricAuthenticator = biometricAuthenticator,
        biometricPromptTitle = "Confirmer ton identité",
    )
    val reauthState: StateFlow<PinAuthUiState> = reauthController.uiState

    private val _revealedProjectIds = MutableStateFlow<Set<Long>>(emptySet())
    val revealedProjectIds: StateFlow<Set<Long>> = _revealedProjectIds.asStateFlow()

    fun beginReauth() = reauthController.reset()

    fun onReauthDigit(digit: Char) = reauthController.onDigit(digit)

    fun onReauthBackspace() = reauthController.onBackspace()

    fun tryReauthBiometric(activity: FragmentActivity) = reauthController.tryBiometric(activity)

    fun revealProject(projectId: Long) {
        _revealedProjectIds.update { it + projectId }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        projectRepository.observeRootProjects().flatMapLatest(::entriesFlow),
        appPreferences.defaultDisplayCurrency,
    ) { entries, currency ->
        HomeUiState(projects = entries, displayCurrency = currency, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun entriesFlow(roots: List<ProjectItem>): Flow<List<ProjectListEntry>> {
        if (roots.isEmpty()) return flowOf(emptyList())
        val rootFlows = roots.map { root ->
            projectRepository.observeChildren(root.id).flatMapLatest { objectives ->
                if (objectives.isEmpty()) return@flatMapLatest flowOf(ProjectListEntry(root, emptyList()))
                val progressFlows = objectives.map { objective ->
                    projectRepository.observeContributions(objective.id).map { contributions ->
                        objective to ProjectProgressCalculator.computeProgress(objective, contributions)
                    }
                }
                combine(progressFlows) { pairs ->
                    ProjectListEntry(root, pairs.toList().sortedBy { it.first.orderIndex })
                }
            }
        }
        return combine(rootFlows) { entries -> entries.toList().sortedBy { it.item.orderIndex } }
    }

    fun setDisplayCurrency(currency: Currency) {
        viewModelScope.launch { appPreferences.setDefaultDisplayCurrency(currency) }
    }

    fun createRootProject(title: String, deadlineAt: Long?, isPrivate: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.createItem(
                ProjectItem(parentId = null, title = trimmed, deadlineAt = deadlineAt, isPrivate = isPrivate),
            )
        }
    }

    fun updateProject(item: ProjectItem, title: String, deadlineAt: Long?, isPrivate: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.updateItem(item.copy(title = trimmed, deadlineAt = deadlineAt, isPrivate = isPrivate))
        }
    }

    fun deleteProject(item: ProjectItem) {
        viewModelScope.launch { projectRepository.deleteItem(item) }
    }
}
