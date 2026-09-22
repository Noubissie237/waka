package com.propentatech.waka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [taskId] reste verrouillée tant que [dependsOnTaskId] n'est pas complétée.
 * Table séparée de l'arbre parent/enfant : une dépendance peut relier deux tâches
 * qui ne sont pas sœurs directes.
 */
@Entity(
    tableName = "task_dependencies",
    foreignKeys = [
        ForeignKey(
            entity = ProjectItem::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProjectItem::class,
            parentColumns = ["id"],
            childColumns = ["dependsOnTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("dependsOnTaskId")],
)
data class TaskDependency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val dependsOnTaskId: Long,
)
