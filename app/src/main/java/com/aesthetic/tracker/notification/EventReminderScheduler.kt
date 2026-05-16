package com.aesthetic.tracker.notification

import com.aesthetic.tracker.data.ImportedScheduleEvent
import java.time.LocalDateTime
import java.time.LocalTime

data class ScheduledEventReminder(
    val event: ImportedScheduleEvent,
    val eventStart: LocalDateTime,
    val reminderAt: LocalDateTime,
)

object EventReminderScheduler {
    fun nextReminder(events: List<ImportedScheduleEvent>, now: LocalDateTime): ScheduledEventReminder? =
        events.asSequence()
            .mapNotNull { event -> event.toReminder(now) }
            .filter { it.eventStart.isAfter(now) }
            .minByOrNull { it.eventStart }

    private fun ImportedScheduleEvent.toReminder(now: LocalDateTime): ScheduledEventReminder? {
        val startTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return null
        val eventStart = LocalDateTime.of(date, startTime)
        val preferredReminderAt = eventStart.minusMinutes(1)
        val reminderAt = if (preferredReminderAt.isAfter(now)) preferredReminderAt else now
        return ScheduledEventReminder(
            event = this,
            eventStart = eventStart,
            reminderAt = reminderAt,
        )
    }
}
