package com.aesthetic.tracker.notification

import com.aesthetic.tracker.data.ImportedScheduleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class EventReminderSchedulerTest {
    private val today = LocalDate.of(2026, 5, 16)

    @Test
    fun `next reminder chooses nearest future event and schedules one minute before start`() {
        val now = LocalDateTime.of(today, LocalTime.of(12, 0))
        val reminder = EventReminderScheduler.nextReminder(
            events = listOf(
                event("dinner", today, "19:00"),
                event("lunch", today, "13:00"),
            ),
            now = now,
        )

        assertEquals("lunch", reminder?.event?.id)
        assertEquals(LocalDateTime.of(today, LocalTime.of(12, 59)), reminder?.reminderAt)
    }

    @Test
    fun `next reminder ignores past events and invalid time`() {
        val now = LocalDateTime.of(today, LocalTime.of(12, 0))
        val reminder = EventReminderScheduler.nextReminder(
            events = listOf(
                event("breakfast", today, "08:00"),
                event("broken", today, "soon"),
                event("walk", today.plusDays(1), "09:00"),
            ),
            now = now,
        )

        assertEquals("walk", reminder?.event?.id)
    }

    @Test
    fun `next reminder fires immediately when event starts in less than one minute`() {
        val now = LocalDateTime.of(today, LocalTime.of(12, 0, 30))
        val reminder = EventReminderScheduler.nextReminder(
            events = listOf(event("checkin", today, "12:01")),
            now = now,
        )

        assertEquals(now, reminder?.reminderAt)
    }

    @Test
    fun `next reminder returns null without future events`() {
        val now = LocalDateTime.of(today, LocalTime.of(12, 0))

        assertNull(EventReminderScheduler.nextReminder(listOf(event("breakfast", today, "08:00")), now))
    }

    private fun event(id: String, date: LocalDate, time: String): ImportedScheduleEvent =
        ImportedScheduleEvent(
            id = id,
            date = date,
            time = time,
            title = id,
            description = null,
            kind = "Other",
            isFixed = false,
            notificationText = null,
        )
}
