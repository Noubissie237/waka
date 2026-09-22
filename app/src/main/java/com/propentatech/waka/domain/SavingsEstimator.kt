package com.propentatech.waka.domain

import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.convert

/**
 * Estimation passive, jamais un plan imposé : à partir du rythme moyen des versements passés,
 * projette une date d'atteinte de l'objectif. Retourne null tant qu'il n'y a pas assez d'historique
 * pour qu'une moyenne veuille dire quelque chose.
 */
object SavingsEstimator {

    private const val MIN_CONTRIBUTIONS = 2
    private const val MIN_SPAN_DAYS = 7
    private const val DAY_MS = 86_400_000L

    data class Estimate(val monthlyAverage: Double, val estimatedDateMillis: Long)

    fun estimate(
        contributions: List<Contribution>,
        targetCurrency: Currency,
        remaining: Double,
        now: Long = System.currentTimeMillis(),
    ): Estimate? {
        if (remaining <= 0.0 || contributions.size < MIN_CONTRIBUTIONS) return null

        val firstDate = contributions.minOf { it.date }
        val spanDays = (now - firstDate) / DAY_MS
        if (spanDays < MIN_SPAN_DAYS) return null

        val totalRaised = contributions.sumOf { it.amount.convert(it.currency, targetCurrency) }
        val monthlyAverage = totalRaised / (spanDays / 30.0)
        if (monthlyAverage <= 0.0) return null

        val monthsNeeded = remaining / monthlyAverage
        val estimatedDateMillis = now + (monthsNeeded * 30 * DAY_MS).toLong()
        return Estimate(monthlyAverage, estimatedDateMillis)
    }
}
