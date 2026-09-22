package com.propentatech.waka.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.propentatech.waka.WakaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager oublie ses alarmes au redémarrage : on replanifie tous les rappels actifs. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as WakaApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.noteRepository.getAllActiveReminders().forEach { reminder ->
                    ReminderScheduler.schedule(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
