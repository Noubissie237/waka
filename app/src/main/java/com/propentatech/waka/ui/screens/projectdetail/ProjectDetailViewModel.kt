package com.propentatech.waka.ui.screens.projectdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.data.prefs.AppPreferences
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.data.repository.ReminderRepository
import com.propentatech.waka.domain.ContributionCascade
import com.propentatech.waka.domain.DependencyEngine
import com.propentatech.waka.domain.ProjectProgress
import com.propentatech.waka.domain.ProjectProgressCalculator
import com.propentatech.waka.domain.ReminderTiming
import com.propentatech.waka.domain.SavingsEstimator
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.model.convert
import com.propentatech.waka.notifications.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChildEntry(val item: ProjectItem, val progress: ProjectProgress, val isLocked: Boolean)

sealed interface ContributionEvent {
    data object Success : ContributionEvent
    data class Rejected(val totalRemaining: Double, val currency: Currency) : ContributionEvent
}

data class ProjectDetailUiState(
    val item: ProjectItem? = null,
    val ownProgress: ProjectProgress? = null,
    val contributions: List<Contribution> = emptyList(),
    val estimate: SavingsEstimator.Estimate? = null,
    val children: List<ChildEntry> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val displayCurrency: Currency = Currency.EUR,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModel(
    private val projectRepository: ProjectRepository,
    private val reminderRepository: ReminderRepository,
    private val appPreferences: AppPreferences,
    private val appContext: Context,
    private val projectItemId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    private val _contributionEvents = Channel<ContributionEvent>(Channel.BUFFERED)
    val contributionEvents: Flow<ContributionEvent> = _contributionEvents.receiveAsFlow()

    init {
        combine(
            projectRepository.observeItem(projectItemId),
            projectRepository.observeContributions(projectItemId),
            childEntriesFlow(projectItemId),
            reminderRepository.observeForProjectItem(projectItemId),
            appPreferences.defaultDisplayCurrency,
        ) { item, contributions, children, reminders, displayCurrency ->
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
                reminders = reminders,
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
            // Les objectifs/tâches atteints descendent en bas : seuls ceux en cours restent bien visibles en haut.
            .sortedBy { it.progress.isCompleted }
    }

    /**
     * Un versement qui dépasse ce qu'il reste à financer sur l'objectif visé déborde automatiquement
     * sur les objectifs suivants du même projet. S'il dépasse ce qu'il reste sur l'ensemble d'entre eux,
     * il est refusé en bloc plutôt que sur-financer silencieusement un objectif imprévu.
     */
    fun addContribution(amount: Double, currency: Currency, dateMillis: Long, note: String?) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val parentId = _uiState.value.item?.parentId
            if (parentId == null) {
                _contributionEvents.send(ContributionEvent.Rejected(0.0, currency))
                return@launch
            }

            val siblings = projectRepository.getChildren(parentId)
                .filter { it.targetAmount != null && it.currency != null }
                .sortedBy { it.orderIndex }
            val siblingStates = siblings.map { sibling ->
                val siblingCurrency = requireNotNull(sibling.currency)
                val raised = projectRepository.getContributions(sibling.id)
                    .sumOf { it.amount.convert(it.currency, siblingCurrency) }
                ContributionCascade.SiblingState(sibling, raised)
            }

            when (val result = ContributionCascade.plan(amount, currency, projectItemId, siblingStates)) {
                is ContributionCascade.Result.Rejected -> {
                    _contributionEvents.send(ContributionEvent.Rejected(result.totalRemaining, result.currency))
                }
                is ContributionCascade.Result.Allocated -> {
                    result.allocations.forEach { (itemId, allocatedAmount) ->
                        val allocatedCurrency = requireNotNull(siblings.first { it.id == itemId }.currency)
                        projectRepository.addContribution(
                            Contribution(
                                projectItemId = itemId,
                                amount = allocatedAmount,
                                currency = allocatedCurrency,
                                date = dateMillis,
                                note = note?.trim()?.ifBlank { null },
                            ),
                        )
                    }
                    _contributionEvents.send(ContributionEvent.Success)
                }
            }
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

    /** Un projet racine n'a ni montant ni devise : seulement un nom, une échéance et sa confidentialité. */
    fun updateSelfAsProject(title: String, deadlineAt: Long?, isPrivate: Boolean) {
        val item = _uiState.value.item ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.updateItem(
                item.copy(title = trimmed, deadlineAt = deadlineAt, isPrivate = isPrivate),
            )
        }
    }

    /** Un objectif/tâche n'a ni échéance ni confidentialité propres : ça se règle au niveau du projet. */
    fun updateSelfAsObjective(title: String, targetAmount: Double?, currency: Currency?) {
        val item = _uiState.value.item ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectRepository.updateItem(item.copy(title = trimmed, targetAmount = targetAmount, currency = currency))
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

    fun addReminder(hour: Int, minute: Int, weekday: Int?, repeatType: RepeatType, message: String) {
        viewModelScope.launch {
            val triggerAt = ReminderTiming.nextOccurrence(repeatType, hour, minute, weekday)
            val reminderId = reminderRepository.create(
                Reminder(
                    projectItemId = projectItemId,
                    triggerAt = triggerAt,
                    message = message.ifBlank { "Rappel" },
                    repeatType = repeatType,
                ),
            )
            reminderRepository.getById(reminderId)?.let { ReminderScheduler.schedule(appContext, it) }
        }
    }

    fun updateReminder(reminder: Reminder, hour: Int, minute: Int, weekday: Int?, repeatType: RepeatType, message: String) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            val triggerAt = ReminderTiming.nextOccurrence(repeatType, hour, minute, weekday)
            val updated = reminder.copy(
                triggerAt = triggerAt,
                message = message.ifBlank { "Rappel" },
                repeatType = repeatType,
                isActive = true,
            )
            reminderRepository.update(updated)
            ReminderScheduler.schedule(appContext, updated)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepository.delete(reminder)
        }
    }
}
