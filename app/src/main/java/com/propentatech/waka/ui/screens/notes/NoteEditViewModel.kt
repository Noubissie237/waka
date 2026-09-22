package com.propentatech.waka.ui.screens.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.Note
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.data.repository.NoteRepository
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteEditUiState(
    val note: Note? = null,
    val reminders: List<Reminder> = emptyList(),
    val isNew: Boolean = true,
)

class NoteEditViewModel(
    private val noteRepository: NoteRepository,
    private val appContext: Context,
    private val noteId: Long?,
) : ViewModel() {

    private var workingId: Long? = noteId

    val uiState: StateFlow<NoteEditUiState> = if (noteId == null) {
        MutableStateFlow(NoteEditUiState(note = null, isNew = true))
    } else {
        combine(
            noteRepository.observeNote(noteId),
            noteRepository.observeRemindersForNote(noteId),
        ) { note, reminders -> NoteEditUiState(note = note, reminders = reminders, isNew = false) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteEditUiState(isNew = noteId == null))

    /** Sauvegarde le titre/contenu ; crée la fiche au premier enregistrement si elle est nouvelle. */
    fun save(title: String, content: String, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = workingId
            if (id == null) {
                val newId = noteRepository.saveNote(Note(title = title, content = content))
                workingId = newId
                onSaved(newId)
            } else {
                val current = uiState.value.note ?: Note(id = id, title = title, content = content)
                noteRepository.updateNote(
                    current.copy(title = title, content = content, updatedAt = System.currentTimeMillis()),
                )
                onSaved(id)
            }
        }
    }

    fun delete() {
        val note = uiState.value.note ?: return
        viewModelScope.launch {
            uiState.value.reminders.forEach { ReminderScheduler.cancel(appContext, it.id) }
            noteRepository.deleteNote(note)
        }
    }

    fun addReminder(triggerAtMillis: Long, message: String, repeatType: RepeatType) {
        val id = workingId ?: return
        viewModelScope.launch {
            val reminderId = noteRepository.createReminder(
                Reminder(noteId = id, triggerAt = triggerAtMillis, message = message, repeatType = repeatType),
            )
            noteRepository.getReminder(reminderId)?.let { ReminderScheduler.schedule(appContext, it) }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            noteRepository.deleteReminder(reminder)
        }
    }
}
