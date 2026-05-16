package com.aesthetic.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aesthetic.tracker.notification.EventProgressNotificationHelper
import com.aesthetic.tracker.ui.AestheticAction
import com.aesthetic.tracker.ui.AestheticApp
import com.aesthetic.tracker.ui.AestheticViewModel
import com.aesthetic.tracker.ui.theme.AestheticTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AestheticViewModel by viewModels {
        val app = application as AestheticTrackerApplication
        AestheticViewModel.Factory(app.repository, app.eventReminderPlanner)
    }
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            AestheticTrackerTheme {
                val state = viewModel.state.collectAsStateWithLifecycle().value
                AestheticApp(
                    state = state,
                    onAction = { action ->
                        if (action is AestheticAction.StartEventProgressNotification) {
                            EventProgressNotificationHelper.show(
                                context = this,
                                eventId = action.eventId,
                                title = action.title,
                                progressPercent = action.progressPercent,
                            )
                        }
                        viewModel.dispatch(action)
                    },
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
