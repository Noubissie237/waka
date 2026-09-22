package com.propentatech.waka.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.propentatech.waka.data.local.entity.Contribution
import kotlinx.coroutines.flow.Flow

@Dao
interface ContributionDao {
    @Insert
    suspend fun insert(contribution: Contribution): Long

    @Update
    suspend fun update(contribution: Contribution)

    @Delete
    suspend fun delete(contribution: Contribution)

    @Query("SELECT * FROM contributions WHERE projectItemId = :projectItemId ORDER BY date DESC")
    fun observeForProjectItem(projectItemId: Long): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions WHERE projectItemId = :projectItemId ORDER BY date DESC")
    suspend fun getForProjectItem(projectItemId: Long): List<Contribution>

    /** Utilisé pour l'export complet. */
    @Query("SELECT * FROM contributions")
    suspend fun getAllContributions(): List<Contribution>
}
