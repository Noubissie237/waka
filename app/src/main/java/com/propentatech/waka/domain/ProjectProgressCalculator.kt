package com.propentatech.waka.domain

import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.model.convert

/**
 * Calcule la progression d'un nœud : complétion manuelle pour les tâches sans budget,
 * complétion automatique par cumul des versements pour les tâches budgétées.
 */
object ProjectProgressCalculator {

    fun computeProgress(item: ProjectItem, contributions: List<Contribution>): ProjectProgress {
        val target = item.targetAmount
        val currency = item.currency
        if (target == null || currency == null) {
            return ProjectProgress(
                isCompleted = item.isManuallyCompleted,
                targetAmount = null,
                currency = null,
                raised = 0.0,
                remaining = null,
                percent = if (item.isManuallyCompleted) 1f else 0f,
            )
        }

        val raised = contributions.sumOf { it.amount.convert(it.currency, currency) }
        val remaining = (target - raised).coerceAtLeast(0.0)
        val percent = if (target > 0.0) (raised / target).toFloat().coerceIn(0f, 1f) else 1f

        return ProjectProgress(
            isCompleted = raised >= target,
            targetAmount = target,
            currency = currency,
            raised = raised,
            remaining = remaining,
            percent = percent,
        )
    }
}
