package com.propentatech.waka.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.propentatech.waka.data.local.entity.TaskDependency
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDependencyDao {
    @Insert
    suspend fun insert(dependency: TaskDependency): Long

    @Delete
    suspend fun delete(dependency: TaskDependency)

    /** Ensemble complet du graphe, utilisé pour la détection de cycle avant d'ajouter une dépendance. */
    @Query("SELECT * FROM task_dependencies")
    suspend fun getAll(): List<TaskDependency>

    /** Prérequis d'une tâche : elle reste verrouillée tant qu'ils ne sont pas tous complétés. */
    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    fun observePrerequisitesOf(taskId: Long): Flow<List<TaskDependency>>

    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    suspend fun getPrerequisitesOf(taskId: Long): List<TaskDependency>

    /** Tâches qui se débloquent quand [dependsOnTaskId] est complétée. */
    @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :dependsOnTaskId")
    suspend fun getDependents(dependsOnTaskId: Long): List<TaskDependency>
}
