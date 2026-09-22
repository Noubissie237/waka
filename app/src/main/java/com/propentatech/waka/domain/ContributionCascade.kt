package com.propentatech.waka.domain

import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.convert

/**
 * Un versement qui dépasse ce qu'il reste à financer sur l'objectif visé déborde automatiquement
 * sur les objectifs suivants du même projet (dans l'ordre), jusqu'à épuisement du montant.
 * Si le montant dépasse ce qu'il reste à financer sur l'ensemble de ces objectifs, rien n'est
 * réparti : mieux vaut refuser clairement que sur-financer silencieusement un autre objectif.
 */
object ContributionCascade {

    /** [raised] déjà exprimé dans la devise propre de [item]. */
    data class SiblingState(val item: ProjectItem, val raised: Double)

    sealed interface Result {
        /** objectifId -> montant à verser, exprimé dans la devise propre de cet objectif. */
        data class Allocated(val allocations: Map<Long, Double>) : Result
        data class Rejected(val totalRemaining: Double, val currency: Currency) : Result
    }

    private const val EPSILON = 0.01

    fun plan(
        amount: Double,
        currency: Currency,
        startItemId: Long,
        orderedSiblings: List<SiblingState>,
    ): Result {
        val startIndex = orderedSiblings.indexOfFirst { it.item.id == startItemId }
        if (startIndex == -1) return Result.Rejected(0.0, currency)

        val targets = orderedSiblings.subList(startIndex, orderedSiblings.size)
        val remainingByItem = targets.associate { sibling ->
            val siblingTarget = requireNotNull(sibling.item.targetAmount)
            sibling.item.id to (siblingTarget - sibling.raised).coerceAtLeast(0.0)
        }
        val totalRemaining = targets.sumOf { sibling ->
            val siblingCurrency = requireNotNull(sibling.item.currency)
            (remainingByItem[sibling.item.id] ?: 0.0).convert(siblingCurrency, currency)
        }

        if (amount > totalRemaining + EPSILON) {
            return Result.Rejected(totalRemaining, currency)
        }

        var remainingToDistribute = amount
        val allocations = mutableMapOf<Long, Double>()
        for (sibling in targets) {
            if (remainingToDistribute <= EPSILON) break
            val siblingCurrency = requireNotNull(sibling.item.currency)
            val siblingRemaining = remainingByItem[sibling.item.id] ?: 0.0
            if (siblingRemaining <= EPSILON) continue

            val availableInSiblingCurrency = remainingToDistribute.convert(currency, siblingCurrency)
            val allocatedInSiblingCurrency = minOf(availableInSiblingCurrency, siblingRemaining)
            if (allocatedInSiblingCurrency > 0.0) {
                allocations[sibling.item.id] = allocatedInSiblingCurrency
                remainingToDistribute -= allocatedInSiblingCurrency.convert(siblingCurrency, currency)
            }
        }
        return Result.Allocated(allocations)
    }
}
