package com.aesthetic.tracker

import android.app.Application
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.AppDatabase
import com.aesthetic.tracker.notification.AlarmEventReminderPlanner
import com.aesthetic.tracker.notification.EventReminderPlanner

class AestheticTrackerApplication : Application() {
    val repository: AestheticRepository by lazy {
        AestheticRepository(AppDatabase.create(this).aestheticDao())
    }
    val eventReminderPlanner: EventReminderPlanner by lazy {
        AlarmEventReminderPlanner(this, repository)
    }
}
