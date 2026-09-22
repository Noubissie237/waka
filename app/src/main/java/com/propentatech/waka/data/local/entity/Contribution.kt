package com.propentatech.waka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.propentatech.waka.model.Currency

/** Un versement libre vers un [ProjectItem] : montant et devise choisis librement, sans périodicité imposée. */
@Entity(
    tableName = "contributions",
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
data class Contribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectItemId: Long,
    val amount: Double,
    val currency: Currency,
    val date: Long,
    val note: String? = null,
)
