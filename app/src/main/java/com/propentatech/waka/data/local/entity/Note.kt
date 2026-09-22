package com.propentatech.waka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fiche texte libre, éventuellement liée à un [ProjectItem] ([projectItemId] nul = fiche autonome). */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectItem::class,
            parentColumns = ["id"],
            childColumns = ["projectItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectItemId")],
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectItemId: Long? = null,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
