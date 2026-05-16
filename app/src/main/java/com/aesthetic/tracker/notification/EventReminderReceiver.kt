package com.aesthetic.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aesthetic.tracker.AestheticTrackerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                when (intent.action) {
                    ActionShowReminder -> {
                        val eventId = intent.getStringExtra(ExtraEventId).orEmpty()
                        val title = intent.getStringExtra(ExtraTitle).orEmpty()
                        val text = intent.getStringExtra(ExtraText)
                        if (eventId.isNotBlank() && title.isNotBlank()) {
                            EventReminderNotificationHelper.show(context, eventId, title, text)
                        }
                        reminderPlanner(context).scheduleNextReminder()
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    -> reminderPlanner(context).scheduleNextReminder()
                }
            }
            pendingResult.finish()
        }
    }

    private fun reminderPlanner(context: Context): EventReminderPlanner =
        (context.applicationContext as AestheticTrackerApplication).eventReminderPlanner

    companion object {
        const val ActionShowReminder = "com.aesthetic.tracker.action.SHOW_EVENT_REMINDER"
        const val ExtraEventId = "event_id"
        const val ExtraTitle = "title"
        const val ExtraText = "text"
    }
}
