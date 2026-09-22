package com.propentatech.waka.ui.screens.projectdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.domain.DependencyEngine
import com.propentatech.waka.domain.ProjectProgress
import com.propentatech.waka.domain.ProjectProgressCalculator
import com.propentatech.waka.domain.SavingsEstimator
import com.propentatech.waka.model.Currency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChildEntry(val item: ProjectItem, val progress: ProjectProgress, val isLocked: Boolean)

data class ProjectDetailUiState(
    val item: ProjectItem? = null,
    val ownProgress: ProjectProgress? = null,
    val contributions: List<Contribution> = emptyList(),
    val estimate: SavingsEstimator.Estimate? = null,
    val children: List<ChildEntry> = emptyList(),
    val displayCurrency: Currency = Currency.EUR,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModel(
    private val projectRepository: ProjectRepository,
    private val appPreferences: AppPreferences,
    private val projectItemId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        combine(
            projectRepository.observeItem(projectItemId),
            projectRepository.observeContributions(projectItemId),
            childEntriesFlow(projectItemId),
            appPreferences.defaultDisplayCurrency,
        ) { item, contributions, children, displayCurrency ->
            val progress = item?.let { ProjectProgressCalculator.computeProgress(it, contributions) }
            val estimate = if (item?.currency != null && progress?.remaining != null) {
                SavingsEstimator.estimate(contributions, item.currency, progress.remaining)
            } else {
                null
            }
            ProjectDetailUiState(
                item = item,
                ownProgress = progress,
                contributions = contributions.sortedByDescending { it.date },
                estimate = estimate,
                children = children,
                displayCurrency = displayCurrency,
                isLoading = false,
            )
        }.onEach { _uiState.value = it }.launchIn(viewModelScope)
    }

    /**
     * Progression + verrouillage de chaque enfant direct. Le verrouillage dépend de la complétion
     * de ses prérequis, qui peuvent se trouver n'importe où dans l'arbre : ils sont donc résolus
     * à chaque recalcul plutôt que seulement parmi les enfants affichés ici.
     */
    private fun childEntriesFlow(parentId: Long) =
        projectRepository.observeChildren(parentId).flatMapLatest { children ->
            if (children.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val progressFlows = children.map { child ->
                projectRepository.observeContributions(child.id)
                    .map { contributions -> child to ProjectProgressCalculator.computeProgress(child, contributions) }
            }

            combine(progressFlows) { it.toList() }.flatMapLatest { childProgress ->
                flow { emit(resolveLockState(childProgress)) }
            }
        }

    private suspend fun resolveLockState(
        childProgress: List<Pair<ProjectItem, ProjectProgress>>,
    ): List<ChildEntry> {
        val prerequisitesByChild = childProgress.associate { (child, _) ->
            child.id to projectRepository.getPrerequisitesOf(child.id)
        }
        val prerequisiteIds = prerequisitesByChild.values.flatten().map { it.dependsOnTaskId }.distinct()
        val completionByPrerequisite = prerequisiteIds.associateWith { id ->
            val prerequisiteItem = projectRepository.getItem(id)
            if (prerequisiteItem == null) {
                true
            } else {
                val contributions = projectRepository.observeContributions(id).first()
                ProjectProgressCalculator.computeProgress(prerequisiteItem, contributions).isCompleted
            }
        }
        return childProgress
            .sortedBy { it.first.orderIndex }
            .map { (child, progress) ->
                val completions = prerequisitesByChild[child.id].orEmpty()
                    .map { completionByPrerequisite[it.dependsOnTaskId] ?: true }
                ChildEntry(child, progress, DependencyEngine.isLocked(completions))
            }
    }

    fun addContribution(amount: Double, currency: Currency, dateMillis: Long, note: String?) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            projectRepository.addContribution(
                Contribution(
                    projectItemId = projectItemId,
                    amount = amount,
                    currency = currency,
                    date = dateMillis,
                    note = note?.trim()?.ifBlank { null },
                ),
            )
        }
    }

    fun deleteContribution(contribution: Contribution) {
        viewModelScope.launch { projectRepository.deleteContribution(contribution) }
    }

    fun toggleManualCompletion(child: ProjectItem) {
        viewModelScope.launch {
            projectRepository.updateItem(child.copy(isManuallyCompleted = !child.isManuallyCompleted))
        }
    }

    fun updateSelf(title: String, targetAmount: Double?, currency: Currency?, isPrivate: Boolean) {
        val item = _uiState.value.item ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.updateItem(
                item.copy(
                    title = trimmed,
                    targetAmount = targetAmount,
                    currency = currency,
                    isPrivate = if (item.parentId == null) isPrivate else item.isPrivate,
                ),
            )
        }
    }

    fun deleteSelf() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch { projectRepository.deleteItem(item) }
    }

    fun updateChild(child: ProjectItem, title: String, targetAmount: Double?, currency: Currency?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.updateItem(child.copy(title = trimmed, targetAmount = targetAmount, currency = currency))
        }
    }

    fun deleteChild(child: ProjectItem) {
        viewModelScope.launch { projectRepository.deleteItem(child) }
    }

    fun createChild(
        title: String,
        targetAmount: Double?,
        currency: Currency?,
        prerequisiteIds: Set<Long>,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val newId = projectRepository.createItem(
                ProjectItem(
                    parentId = projectItemId,
                    title = trimmed,
                    targetAmount = targetAmount,
                    currency = currency,
                    orderIndex = _uiState.value.children.size,
                ),
            )
            prerequisiteIds.forEach { prerequisiteId ->
                projectRepository.addDependency(taskId = newId, dependsOnTaskId = prerequisiteId)
            }
        }
    }
}
