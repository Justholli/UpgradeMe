package com.aesthetic.tracker

import android.app.Application
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.AppDatabase

class AestheticTrackerApplication : Application() {
    val repository: AestheticRepository by lazy {
        AestheticRepository(AppDatabase.create(this).aestheticDao())
    }
}
