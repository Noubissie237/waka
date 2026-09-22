package com.propentatech.waka.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.propentatech.waka.R
import com.propentatech.waka.data.local.entity.Reminder

// v2 : nouveau canal pour appliquer le son personnalisé, le son d'un canal existant est
// immuable une fois créé, un simple changement de CHANNEL_ID force sa recréation.
private const val CHANNEL_ID = "waka_reminders_v2"
private const val LEGACY_CHANNEL_ID = "waka_reminders"

object NotificationHelper {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val soundUri = "android.resource://${context.packageName}/${R.raw.notification}".toUri()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rappels",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Rappels programmés depuis le bloc-notes et les échéances de projet."
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, reminder: Reminder) {
        ensureChannel(context)
        val soundUri = "android.resource://${context.packageName}/${R.raw.notification}".toUri()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Waka")
            .setContentText(reminder.message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri) // ignoré sur API 26+ (c'est le canal qui décide), utilisé en dessous
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }
}
