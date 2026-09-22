package com.propentatech.waka.data.backup

import kotlinx.serialization.Serializable

/**
 * Format d'export JSON, indépendant du schéma Room : `localId` ne sert qu'à recoller les
 * relations (parent, versements, dépendances, rappels) entre elles à l'import, où de nouveaux
 * identifiants sont générés pour ne jamais entrer en collision avec les données déjà présentes.
 */
@Serializable
data class ProjectItemDto(
    val localId: Long,
    val parentLocalId: Long? = null,
    val title: String,
    val description: String? = null,
    val targetAmount: Double? = null,
    val currency: String? = null,
    val isManuallyCompleted: Boolean = false,
    val isPrivate: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long,
    val deadlineAt: Long? = null,
)

@Serializable
data class ContributionDto(
    val projectItemLocalId: Long,
    val amount: Double,
    val currency: String,
    val date: Long,
    val note: String? = null,
)

@Serializable
data class TaskDependencyDto(
    val taskLocalId: Long,
    val dependsOnLocalId: Long,
)

@Serializable
data class ReminderDto(
    val projectItemLocalId: Long,
    val triggerAt: Long,
    val message: String,
    val isActive: Boolean = true,
    val repeatType: String = "NONE",
)

@Serializable
data class WakaBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val projectItems: List<ProjectItemDto> = emptyList(),
    val contributions: List<ContributionDto> = emptyList(),
    val dependencies: List<TaskDependencyDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
)
