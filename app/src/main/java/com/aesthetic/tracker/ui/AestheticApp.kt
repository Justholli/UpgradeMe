package com.aesthetic.tracker.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.aesthetic.tracker.R
import com.aesthetic.tracker.ui.dashboard.CalendarScreen
import com.aesthetic.tracker.ui.dashboard.ProgressScreen
import com.aesthetic.tracker.ui.dashboard.TodayScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AestheticApp(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TrackerScreen.entries.forEach { screen ->
                    val label = stringResource(screen.labelRes)
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${screen.name}"),
                        selected = state.selectedScreen == screen,
                        onClick = { onAction(AestheticAction.SelectScreen(screen)) },
                        icon = { Icon(screen.icon(), contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Crossfade(targetState = state.selectedScreen, label = "dashboard_screen_transition") { screen ->
                    when (screen) {
                        TrackerScreen.Today -> TodayScreen(state = state, onAction = onAction)
                        TrackerScreen.Calendar -> CalendarScreen(state = state)
                        TrackerScreen.Progress -> ProgressScreen(state = state, onAction = onAction)
                    }
                }
            }
        }
    }
}

private fun TrackerScreen.icon(): ImageVector = when (this) {
    TrackerScreen.Today -> Icons.Default.Today
    TrackerScreen.Calendar -> Icons.Default.CalendarMonth
    TrackerScreen.Progress -> Icons.Default.Insights
}
