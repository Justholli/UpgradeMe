package com.aesthetic.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aesthetic.tracker.ui.AestheticApp
import com.aesthetic.tracker.ui.AestheticViewModel
import com.aesthetic.tracker.ui.theme.AestheticTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AestheticViewModel by viewModels {
        AestheticViewModel.Factory((application as AestheticTrackerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AestheticTrackerTheme {
                val state = viewModel.state.collectAsStateWithLifecycle().value
                AestheticApp(state = state, onAction = viewModel::dispatch)
            }
        }
    }
}
