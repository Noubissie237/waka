package com.propentatech.waka.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.propentatech.waka.data.local.entity.ProjectItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ProjectItem): Long

    @Update
    suspend fun update(item: ProjectItem)

    @Delete
    suspend fun delete(item: ProjectItem)

    @Query("SELECT * FROM project_items WHERE id = :id")
    fun observeById(id: Long): Flow<ProjectItem?>

    @Query("SELECT * FROM project_items WHERE id = :id")
    suspend fun getById(id: Long): ProjectItem?

    @Query("SELECT * FROM project_items WHERE parentId IS NULL ORDER BY orderIndex")
    fun observeRootProjects(): Flow<List<ProjectItem>>

    @Query("SELECT * FROM project_items WHERE parentId = :parentId ORDER BY orderIndex")
    fun observeChildren(parentId: Long): Flow<List<ProjectItem>>

    @Query("SELECT * FROM project_items WHERE parentId = :parentId ORDER BY orderIndex")
    suspend fun getChildren(parentId: Long): List<ProjectItem>

    /** Utilisé pour l'export complet : tout l'arbre, sans distinction de niveau. */
    @Query("SELECT * FROM project_items")
    suspend fun getAllItems(): List<ProjectItem>
}
