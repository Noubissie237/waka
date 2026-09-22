package com.propentatech.waka.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.propentatech.waka.R
import com.propentatech.waka.data.local.entity.Reminder

private const val CHANNEL_ID = "waka_reminders"

object NotificationHelper {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rappels",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Rappels programmés depuis le bloc-notes et les échéances de projet." }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, reminder: Reminder) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Waka")
            .setContentText(reminder.message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }
}
