package com.aesthetic.tracker.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.aesthetic.tracker.domain.TodayScheduleItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleToday(items: List<TodayScheduleItem>, today: LocalDate = LocalDate.now()): Boolean {
        if (!canPostNotifications()) return false

        items.forEach { item ->
            val triggerAt = today.atTime(item.time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (triggerAt <= System.currentTimeMillis()) return@forEach

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(item),
            )
        }
        return true
    }

    private fun pendingIntent(item: TodayScheduleItem): PendingIntent {
        val intent = Intent(context, TodayNotificationReceiver::class.java).apply {
            putExtra(TodayNotificationReceiver.ExtraTitle, item.title)
            putExtra(TodayNotificationReceiver.ExtraText, item.notificationText)
            putExtra(TodayNotificationReceiver.ExtraId, item.id.hashCode())
        }
        return PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
