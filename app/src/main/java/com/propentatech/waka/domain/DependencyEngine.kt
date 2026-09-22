package com.propentatech.waka.domain

import com.propentatech.waka.data.local.entity.TaskDependency

/** Règles pures autour des dépendances entre tâches : verrouillage et prévention des cycles. */
object DependencyEngine {

    /** Une tâche reste verrouillée tant qu'un de ses prérequis n'est pas complété. */
    fun isLocked(prerequisiteCompletions: List<Boolean>): Boolean =
        prerequisiteCompletions.any { !it }

    /**
     * Est-ce qu'ajouter "[taskId] dépend de [dependsOnTaskId]" créerait un cycle,
     * directement ou via une chaîne de dépendances existantes ?
     */
    fun wouldCreateCycle(
        taskId: Long,
        dependsOnTaskId: Long,
        existingDependencies: List<TaskDependency>,
    ): Boolean {
        if (taskId == dependsOnTaskId) return true

        val prerequisitesOf: Map<Long, List<Long>> =
            existingDependencies.groupBy({ it.taskId }, { it.dependsOnTaskId })
        val visited = mutableSetOf<Long>()

        fun reachesTask(from: Long): Boolean {
            if (from == taskId) return true
            if (!visited.add(from)) return false
            return prerequisitesOf[from].orEmpty().any { reachesTask(it) }
        }

        return reachesTask(dependsOnTaskId)
    }
}
