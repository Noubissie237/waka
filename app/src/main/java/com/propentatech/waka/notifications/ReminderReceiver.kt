package com.propentatech.waka.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.propentatech.waka.WakaApplication
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.domain.ReminderTiming
import com.propentatech.waka.model.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reçoit l'alarme système au moment prévu, affiche la notification et replanifie si récurrent. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = ReminderScheduler.extractReminderId(intent)
        if (reminderId < 0) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as WakaApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = container.reminderRepository.getById(reminderId)
                if (reminder != null && reminder.isActive) {
                    NotificationHelper.show(context, reminder)
                    rescheduleIfRepeating(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleIfRepeating(context: Context, reminder: Reminder) {
        val container = (context.applicationContext as WakaApplication).container
        if (reminder.repeatType == RepeatType.NONE) {
            container.reminderRepository.update(reminder.copy(isActive = false))
            return
        }
        val updated = reminder.copy(triggerAt = ReminderTiming.advance(reminder.triggerAt, reminder.repeatType))
        container.reminderRepository.update(updated)
        ReminderScheduler.schedule(context, updated)
    }
}
