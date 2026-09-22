package com.propentatech.waka.ui.screens.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.data.repository.ReminderRepository
import com.propentatech.waka.domain.ReminderTiming
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.notifications.ReminderScheduler
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

/** [parentTitle] est non nul quand le rappel porte sur un objectif (l'enfant d'un projet). */
data class ReminderWithContext(val reminder: Reminder, val itemTitle: String, val parentTitle: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val projectRepository: ProjectRepository,
    private val appContext: Context,
) : ViewModel() {

    val reminders: StateFlow<List<ReminderWithContext>> = reminderRepository.observeActive()
        .flatMapLatest(::withContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun withContext(reminders: List<Reminder>): Flow<List<ReminderWithContext>> {
        if (reminders.isEmpty()) return flowOf(emptyList())
        val flows = reminders.map { reminder ->
            projectRepository.observeItem(reminder.projectItemId).flatMapLatest { item ->
                when {
                    item == null -> flowOf(null)
                    item.parentId == null -> flowOf(ReminderWithContext(reminder, item.title, null))
                    else -> projectRepository.observeItem(item.parentId).map { parent ->
                        ReminderWithContext(reminder, item.title, parent?.title)
                    }
                }
            }
        }
        return combine(flows) { arr -> arr.filterNotNull().sortedBy { it.reminder.triggerAt } }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepository.delete(reminder)
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
}
