package com.propentatech.waka.data.repository

import com.propentatech.waka.data.local.dao.ContributionDao
import com.propentatech.waka.data.local.dao.ProjectItemDao
import com.propentatech.waka.data.local.dao.TaskDependencyDao
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.local.entity.TaskDependency
import com.propentatech.waka.domain.DependencyEngine
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectItemDao: ProjectItemDao,
    private val contributionDao: ContributionDao,
    private val taskDependencyDao: TaskDependencyDao,
) {
    fun observeRootProjects(): Flow<List<ProjectItem>> = projectItemDao.observeRootProjects()

    fun observeChildren(parentId: Long): Flow<List<ProjectItem>> = projectItemDao.observeChildren(parentId)

    suspend fun getChildren(parentId: Long): List<ProjectItem> = projectItemDao.getChildren(parentId)

    suspend fun getAllItems(): List<ProjectItem> = projectItemDao.getAllItems()

    fun observeItem(id: Long): Flow<ProjectItem?> = projectItemDao.observeById(id)

    suspend fun getItem(id: Long): ProjectItem? = projectItemDao.getById(id)

    suspend fun createItem(item: ProjectItem): Long = projectItemDao.insert(item)

    suspend fun updateItem(item: ProjectItem) = projectItemDao.update(item)

    suspend fun deleteItem(item: ProjectItem) = projectItemDao.delete(item)

    fun observeContributions(projectItemId: Long): Flow<List<Contribution>> =
        contributionDao.observeForProjectItem(projectItemId)

    suspend fun getContributions(projectItemId: Long): List<Contribution> =
        contributionDao.getForProjectItem(projectItemId)

    suspend fun getAllContributions(): List<Contribution> = contributionDao.getAllContributions()

    suspend fun addContribution(contribution: Contribution): Long = contributionDao.insert(contribution)

    suspend fun updateContribution(contribution: Contribution) = contributionDao.update(contribution)

    suspend fun deleteContribution(contribution: Contribution) = contributionDao.delete(contribution)

    fun observePrerequisitesOf(taskId: Long): Flow<List<TaskDependency>> =
        taskDependencyDao.observePrerequisitesOf(taskId)

    suspend fun getPrerequisitesOf(taskId: Long): List<TaskDependency> =
        taskDependencyDao.getPrerequisitesOf(taskId)

    suspend fun getDependents(taskId: Long): List<TaskDependency> = taskDependencyDao.getDependents(taskId)

    suspend fun getAllDependencies(): List<TaskDependency> = taskDependencyDao.getAll()

    /** @return false sans rien écrire si la dépendance créerait un cycle. */
    suspend fun addDependency(taskId: Long, dependsOnTaskId: Long): Boolean {
        val allDependencies = taskDependencyDao.getAll()
        if (DependencyEngine.wouldCreateCycle(taskId, dependsOnTaskId, allDependencies)) {
            return false
        }
        taskDependencyDao.insert(TaskDependency(taskId = taskId, dependsOnTaskId = dependsOnTaskId))
        return true
    }

    suspend fun removeDependency(dependency: TaskDependency) = taskDependencyDao.delete(dependency)
}
