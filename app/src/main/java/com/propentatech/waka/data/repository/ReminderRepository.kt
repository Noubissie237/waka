package com.propentatech.waka.data.repository

import com.propentatech.waka.data.local.dao.ReminderDao
import com.propentatech.waka.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    fun observeActive(): Flow<List<Reminder>> = reminderDao.observeActive()

    suspend fun getAllActive(): List<Reminder> = reminderDao.getAllActive()

    fun observeForProjectItem(projectItemId: Long): Flow<List<Reminder>> =
        reminderDao.observeForProjectItem(projectItemId)

    suspend fun getById(id: Long): Reminder? = reminderDao.getById(id)

    suspend fun create(reminder: Reminder): Long = reminderDao.insert(reminder)

    suspend fun update(reminder: Reminder) = reminderDao.update(reminder)

    suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
}
