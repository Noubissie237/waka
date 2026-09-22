package com.propentatech.waka.domain

import com.propentatech.waka.model.Currency

/**
 * État dérivé d'un [com.propentatech.waka.data.local.entity.ProjectItem], recalculé à la lecture —
 * jamais stocké, pour n'avoir qu'une seule source de vérité (les versements).
 */
data class ProjectProgress(
    val isCompleted: Boolean,
    val targetAmount: Double?,
    val currency: Currency?,
    val raised: Double,
    val remaining: Double?,
    val percent: Float,
)
