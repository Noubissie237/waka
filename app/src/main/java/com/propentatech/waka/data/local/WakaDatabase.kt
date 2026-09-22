package com.propentatech.waka.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.propentatech.waka.data.local.dao.ContributionDao
import com.propentatech.waka.data.local.dao.ProjectItemDao
import com.propentatech.waka.data.local.dao.ReminderDao
import com.propentatech.waka.data.local.dao.TaskDependencyDao
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.data.local.entity.TaskDependency

@Database(
    entities = [
        ProjectItem::class,
        Contribution::class,
        TaskDependency::class,
        Reminder::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WakaDatabase : RoomDatabase() {
    abstract fun projectItemDao(): ProjectItemDao
    abstract fun contributionDao(): ContributionDao
    abstract fun taskDependencyDao(): TaskDependencyDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var instance: WakaDatabase? = null

        fun getInstance(context: Context): WakaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WakaDatabase::class.java,
                    "waka.db",
                )
                    // App encore en développement, pas de données à préserver entre schémas :
                    // une vraie migration remplacera ceci avant la première publication.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
    }
}
