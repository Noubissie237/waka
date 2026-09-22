package com.propentatech.waka.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.domain.ProjectProgress
import com.propentatech.waka.domain.ProjectProgressCalculator
import com.propentatech.waka.model.Currency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectListEntry(val item: ProjectItem, val progress: ProjectProgress)

data class HomeUiState(
    val projects: List<ProjectListEntry> = emptyList(),
    val displayCurrency: Currency = Currency.EUR,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val projectRepository: ProjectRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        projectRepository.observeRootProjects().flatMapLatest(::entriesFlow),
        appPreferences.defaultDisplayCurrency,
    ) { entries, currency ->
        HomeUiState(projects = entries, displayCurrency = currency, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    // Le badge de progression d'un projet racine sans budget propre (un simple dossier de
    // sous-tâches) sera calculé sur toute l'arborescence à l'étape 6 ; pour l'instant seul le
    // budget direct du nœud racine est pris en compte.
    private fun entriesFlow(items: List<ProjectItem>): Flow<List<ProjectListEntry>> {
        if (items.isEmpty()) return flowOf(emptyList())
        val entryFlows = items.map { item ->
            projectRepository.observeContributions(item.id).map { contributions ->
                ProjectListEntry(item, ProjectProgressCalculator.computeProgress(item, contributions))
            }
        }
        return combine(entryFlows) { entries -> entries.sortedBy { it.item.orderIndex } }
    }

    fun setDisplayCurrency(currency: Currency) {
        viewModelScope.launch { appPreferences.setDefaultDisplayCurrency(currency) }
    }

    fun createRootProject(title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.createItem(
                ProjectItem(
                    parentId = null,
                    title = trimmed,
                    targetAmount = targetAmount,
                    currency = currency,
                    isPrivate = isPrivate,
                ),
            )
        }
    }

    fun updateProject(item: ProjectItem, title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.updateItem(
                item.copy(title = trimmed, targetAmount = targetAmount, currency = currency, isPrivate = isPrivate),
            )
        }
    }

    fun deleteProject(item: ProjectItem) {
        viewModelScope.launch { projectRepository.deleteItem(item) }
    }
}
