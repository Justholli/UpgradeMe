package com.aesthetic.tracker.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.ui.AestheticAction
import com.aesthetic.tracker.ui.AestheticState
import com.aesthetic.tracker.ui.theme.AestheticTrackerTheme

@Composable
fun TodayScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    val day = state.dashboard.today
    var selectedEvent by remember { mutableStateOf<ScheduleEventUi?>(null) }
    var currentCollapsed by rememberSaveable { mutableStateOf(false) }
    var nextCollapsed by rememberSaveable { mutableStateOf(false) }
    val currentEvent = DashboardUiMapper.currentEvent(day)
    val nextEvent = DashboardUiMapper.nextUpcomingEvent(day)
    val mealEvent = currentEvent?.takeIf { it.mealRecommendation != null }
        ?: nextEvent?.takeIf { it.mealRecommendation != null }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen_Today"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DashboardCard(elevated = true) {
                Text("Сегодня", fontWeight = FontWeight.Bold)
                Text("${day.dayOfWeek}, ${day.date}")
                day.description?.let { Text(it) }
            }
        }
        item(key = "currentEvent") {
            AnimatedVisibility(
                visible = currentEvent != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                currentEvent?.let { event ->
                    EventHighlightCard(
                        event = event,
                        title = "Текущее событие",
                        emptyTitle = "",
                        emptyBody = "",
                        testTag = "currentEventCard",
                        collapsed = currentCollapsed,
                        showProgressAction = true,
                        onCollapsedChange = { currentCollapsed = it },
                        onProgressClick = {
                            if (it.hasStarted) {
                                onAction(AestheticAction.ToggleScheduleEventCompletion(it.id))
                            } else {
                                onAction(
                                    AestheticAction.StartEventProgressNotification(
                                        eventId = it.id,
                                        title = it.title,
                                        progressPercent = it.eventProgressPercent ?: 0,
                                    ),
                                )
                            }
                        },
                        onEventClick = { selectedEvent = it },
                    )
                }
            }
        }
        item {
            EventHighlightCard(
                event = nextEvent,
                title = "Следующее событие",
                emptyTitle = "На сегодня событий больше нет",
                emptyBody = "Все запланированные действия уже закрыты.",
                testTag = "nextEventCard",
                collapsed = nextCollapsed,
                showProgressAction = false,
                onCollapsedChange = { nextCollapsed = it },
                onProgressClick = {},
                onEventClick = { selectedEvent = it },
            )
        }
        item(key = "mealRecommendation") {
            AnimatedVisibility(
                visible = mealEvent?.mealRecommendation != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                mealEvent?.mealRecommendation?.let { meal ->
                    MealRecommendationCard(meal = meal)
                }
            }
        }
        item { SectionTitle("Таймлайн") }
        item {
            DayEventsList(
                events = day.events,
                onEventClick = { selectedEvent = it },
            )
        }
        item { CompletedTasksCard(day) }
        item { DailyStatsCard(day) }
    }

    selectedEvent?.let { event ->
        EventDetailsBottomSheet(
            event = event,
            onDismiss = { selectedEvent = null },
            onToggle = { onAction(AestheticAction.ToggleScheduleEventCompletion(it.id)) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    AestheticTrackerTheme {
        TodayScreen(
            state = AestheticState(isLoading = false).let { it.copy(dashboard = DashboardUiMapper.map(it)) },
            onAction = {},
        )
    }
}
