package com.aesthetic.tracker.notification

interface EventReminderPlanner {
    suspend fun scheduleNextReminder()
    fun cancelReminder()
}

object NoOpEventReminderPlanner : EventReminderPlanner {
    override suspend fun scheduleNextReminder() = Unit
    override fun cancelReminder() = Unit
}
