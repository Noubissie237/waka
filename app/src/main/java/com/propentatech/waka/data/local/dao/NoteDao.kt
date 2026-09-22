package com.propentatech.waka.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.propentatech.waka.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE projectItemId = :projectItemId ORDER BY updatedAt DESC")
    fun observeForProjectItem(projectItemId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<Note?>
}
