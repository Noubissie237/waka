package com.propentatech.waka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.propentatech.waka.model.Currency

/**
 * Un seul arbre auto-référencé porte projets, phases, tâches et sous-tâches.
 * [parentId] nul = projet racine. Un nœud budgété ([targetAmount] non nul) se complète
 * par cumul de [com.propentatech.waka.data.local.entity.Contribution], jamais par [isManuallyCompleted].
 */
@Entity(
    tableName = "project_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectItem::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentId")],
)
data class ProjectItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long? = null,
    val title: String,
    val description: String? = null,
    val targetAmount: Double? = null,
    val currency: Currency? = null,
    val isManuallyCompleted: Boolean = false,
    val isPrivate: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val deadlineAt: Long? = null,
)
