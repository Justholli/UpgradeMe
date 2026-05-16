package com.aesthetic.tracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aesthetic.tracker.data.AestheticRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmEventReminderPlanner(
    private val context: Context,
    private val repository: AestheticRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : EventReminderPlanner {
    private val appContext = context.applicationContext
    private val alarmManager: AlarmManager
        get() = appContext.getSystemService(AlarmManager::class.java)

    override suspend fun scheduleNextReminder() = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now(clock)
        val reminder = EventReminderScheduler.nextReminder(repository.getImportedScheduleEvents(), now)
        if (reminder == null) {
            cancelReminder()
            return@withContext
        }
        val triggerAtMillis = reminder.reminderAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val pendingIntent = reminderPendingIntent(
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            eventId = reminder.event.id,
            title = reminder.event.title,
            text = reminder.event.notificationText ?: reminder.event.description,
        ) ?: return@withContext
        alarmManager.cancel(pendingIntent)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override fun cancelReminder() {
        val pendingIntent = reminderPendingIntent(flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun reminderPendingIntent(
        flags: Int,
        eventId: String? = null,
        title: String? = null,
        text: String? = null,
    ): PendingIntent? {
        val intent = Intent(appContext, EventReminderReceiver::class.java)
            .setAction(EventReminderReceiver.ActionShowReminder)
        if (eventId != null && title != null) {
            intent.putExtra(EventReminderReceiver.ExtraEventId, eventId)
            intent.putExtra(EventReminderReceiver.ExtraTitle, title)
            intent.putExtra(EventReminderReceiver.ExtraText, text)
        }
        return PendingIntent.getBroadcast(appContext, ReminderRequestCode, intent, flags)
    }

    private companion object {
        const val ReminderRequestCode = 20260517
    }
}
