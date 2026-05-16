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

object EventProgressNotificationHelper {
    private const val ChannelId = "event_progress"
    private const val NotificationId = 20260516

    fun show(context: Context, eventId: String, title: String, progressPercent: Int) {
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
            .setSmallIcon(R.drawable.ic_event_workout)
            .setContentTitle(title)
            .setContentText("Выполняется: ${progressPercent.coerceIn(0, 100)}%")
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationId, notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ChannelId,
            "Прогресс текущего события",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Показывает прогресс выполнения текущего события"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }
}
