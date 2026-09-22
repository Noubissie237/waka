package com.propentatech.waka.domain

import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.convert

/**
 * Total d'un projet : la somme des montants cibles (et versements) de ses objectifs directs,
 * chacun converti dans [Currency] d'affichage avant sommation, jamais après, sous peine de
 * mélanger des unités différentes.
 */
data class ProjectAggregate(
    val totalTarget: Double,
    val totalRaised: Double,
    val hasObjectives: Boolean,
) {
    val percent: Float
        get() = if (totalTarget > 0.0) (totalRaised / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = hasObjectives && totalRaised >= totalTarget
}

object ProjectAggregator {
    fun aggregate(objectives: List<Pair<ProjectItem, ProjectProgress>>, displayCurrency: Currency): ProjectAggregate {
        var totalTarget = 0.0
        var totalRaised = 0.0
        var hasObjectives = false
        for ((item, progress) in objectives) {
            val currency = item.currency
            val target = item.targetAmount
            if (currency != null && target != null) {
                hasObjectives = true
                totalTarget += target.convert(currency, displayCurrency)
                totalRaised += progress.raised.convert(currency, displayCurrency)
            }
        }
        return ProjectAggregate(totalTarget, totalRaised, hasObjectives)
    }
}
