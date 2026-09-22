package com.propentatech.waka.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.propentatech.waka.data.local.entity.Reminder

private const val EXTRA_REMINDER_ID = "reminder_id"

/** Planifie/annule les alarmes système ; le déclenchement réel est géré par [ReminderReceiver]. */
object ReminderScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.isActive || reminder.triggerAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntentFor(context, reminder.id)

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntentFor(context, reminderId))
    }

    fun extractReminderId(intent: Intent): Long = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

    private fun pendingIntentFor(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_REMINDER_ID, reminderId)
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
