package com.aesthetic.tracker.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aesthetic.tracker.MainActivity
import com.aesthetic.tracker.R

object EventReminderNotificationHelper {
    private const val ChannelId = "event_reminders"
    private const val NotificationIdBase = 20260517

    fun show(context: Context, eventId: String, title: String, text: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_event_import)
            .setContentTitle("Скоро: $title")
            .setContentText(text ?: "Событие начнется через минуту.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text ?: "Событие начнется через минуту."))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationIdBase + eventId.hashCode(), notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ChannelId,
            "Напоминания о событиях",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Уведомляет за минуту до начала следующего события"
        }
        manager.createNotificationChannel(channel)
    }
}
