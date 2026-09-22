package com.propentatech.waka.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.propentatech.waka.data.local.dao.ContributionDao
import com.propentatech.waka.data.local.dao.NoteDao
import com.propentatech.waka.data.local.dao.ProjectItemDao
import com.propentatech.waka.data.local.dao.ReminderDao
import com.propentatech.waka.data.local.dao.TaskDependencyDao
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.Note
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.data.local.entity.TaskDependency

@Database(
    entities = [
        ProjectItem::class,
        Contribution::class,
        TaskDependency::class,
        Note::class,
        Reminder::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WakaDatabase : RoomDatabase() {
    abstract fun projectItemDao(): ProjectItemDao
    abstract fun contributionDao(): ContributionDao
    abstract fun taskDependencyDao(): TaskDependencyDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var instance: WakaDatabase? = null

        fun getInstance(context: Context): WakaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WakaDatabase::class.java,
                    "waka.db",
                ).build().also { instance = it }
            }
    }
}
