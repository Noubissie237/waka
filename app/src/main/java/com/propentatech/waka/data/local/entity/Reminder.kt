package com.propentatech.waka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.propentatech.waka.model.RepeatType

/**
 * Rappel programmé via AlarmManager, déclenché même app fermée.
 * Lié à une [Note] et/ou directement à un [ProjectItem] (ex : rappel d'échéance auto-généré).
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProjectItem::class,
            parentColumns = ["id"],
            childColumns = ["projectItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId"), Index("projectItemId")],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long? = null,
    val projectItemId: Long? = null,
    val triggerAt: Long,
    val message: String,
    val isActive: Boolean = true,
    val repeatType: RepeatType = RepeatType.NONE,
)
