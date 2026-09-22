package com.propentatech.waka.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.propentatech.waka.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY triggerAt")
    fun observeActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getAllActive(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE projectItemId = :projectItemId ORDER BY triggerAt")
    fun observeForProjectItem(projectItemId: Long): Flow<List<Reminder>>
}
