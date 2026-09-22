package com.propentatech.waka.data.repository

import com.propentatech.waka.data.local.dao.NoteDao
import com.propentatech.waka.data.local.dao.ReminderDao
import com.propentatech.waka.data.local.entity.Note
import com.propentatech.waka.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
) {
    fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAll()

    fun observeNotesFor(projectItemId: Long): Flow<List<Note>> = noteDao.observeForProjectItem(projectItemId)

    fun observeNote(id: Long): Flow<Note?> = noteDao.observeById(id)

    suspend fun saveNote(note: Note): Long = noteDao.insert(note)

    suspend fun updateNote(note: Note) = noteDao.update(note)

    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    fun observeActiveReminders(): Flow<List<Reminder>> = reminderDao.observeActive()

    suspend fun getReminder(id: Long): Reminder? = reminderDao.getById(id)

    suspend fun getAllActiveReminders(): List<Reminder> = reminderDao.getAllActive()

    fun observeRemindersForNote(noteId: Long): Flow<List<Reminder>> = reminderDao.observeForNote(noteId)

    fun observeRemindersForProjectItem(projectItemId: Long): Flow<List<Reminder>> =
        reminderDao.observeForProjectItem(projectItemId)

    suspend fun createReminder(reminder: Reminder): Long = reminderDao.insert(reminder)

    suspend fun updateReminder(reminder: Reminder) = reminderDao.update(reminder)

    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)

    /** Rappels d'échéance auto-générés d'une tâche (hors rappels manuels liés à une fiche). */
    suspend fun getAutoDeadlineReminders(projectItemId: Long): List<Reminder> =
        reminderDao.getAutoDeadlineReminders(projectItemId)
}
