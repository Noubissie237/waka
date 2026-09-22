package com.propentatech.waka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.propentatech.waka.model.RepeatType

/**
 * Rappel programmé via AlarmManager, déclenché même app fermée.
 * Toujours rattaché à un projet ou à un de ses objectifs, Waka ne gère pas de notes libres.
 */
@Entity(
    tableName = "reminders",
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
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectItemId: Long,
    val triggerAt: Long,
    val message: String,
    val isActive: Boolean = true,
    val repeatType: RepeatType = RepeatType.NONE,
)
