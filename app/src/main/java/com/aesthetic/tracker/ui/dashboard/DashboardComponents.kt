@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aesthetic.tracker.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.R

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
internal fun DashboardCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
    val colors = CardDefaults.cardColors(
        containerColor = if (elevated) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    )
    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            content = { cardContent() },
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            onClick = onClick,
            content = { cardContent() },
        )
    }
}

@Composable
internal fun EmptyStateCard(title: String, body: String) {
    DashboardCard(elevated = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EventTypeIcon(type = ScheduleEventType.Other, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun EventTypeIcon(type: ScheduleEventType, contentDescription: String?) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(
            painter = painterResource(type.drawableRes()),
            contentDescription = contentDescription,
            modifier = Modifier.padding(9.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun EventStatusChip(status: ScheduleEventStatus) {
    AssistChip(onClick = {}, label = {
        Crossfade(targetState = status, label = "eventStatusChip") { targetStatus ->
            Text(
                when (targetStatus) {
                    ScheduleEventStatus.Upcoming -> "Скоро"
                    ScheduleEventStatus.Active -> "Сейчас"
                    ScheduleEventStatus.Completed -> "Готово"
                    ScheduleEventStatus.Missed -> "Пропущено"
                },
            )
        }
    })
}

@Composable
private fun EventTitle(title: String, style: androidx.compose.ui.text.TextStyle) {
    Crossfade(targetState = title, label = "eventTitle") { targetTitle ->
        Text(targetTitle, style = style, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun EventHighlightCard(
    event: ScheduleEventUi?,
    title: String,
    emptyTitle: String,
    emptyBody: String,
    testTag: String,
    collapsed: Boolean,
    showProgressAction: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    onProgressClick: (ScheduleEventUi) -> Unit,
    onEventClick: (ScheduleEventUi) -> Unit,
) {
    if (event == null) {
        EmptyStateCard(title = emptyTitle, body = emptyBody)
        return
    }
    DashboardCard(
        modifier = Modifier.testTag(testTag),
        elevated = true,
        onClick = if (event.canOpenDetails) ({ onEventClick(event) }) else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCollapsedChange(!collapsed) }
                    .testTag("${testTag}_collapseButton"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!collapsed) {
                    EventTypeIcon(event.type, event.title)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (!collapsed) Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EventTitle(event.title, style = MaterialTheme.typography.headlineSmall)
                }
                if (!collapsed) {
                    event.timeUntilStartText?.let {
                        CountdownRing(
                            timeText = it,
                            progressPercent = event.timeUntilStartProgressPercent ?: 0,
                        )
                    } ?: ProgressRing(percent = event.eventProgressPercent ?: if (event.isCompleted) 100 else 50)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (collapsed) "Развернуть" else "Свернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = !collapsed,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        EventStatusChip(event.status)
                        if (event.hasStarted && !event.isCompleted) {
                            AssistChip(onClick = {}, label = { Text("В работе") })
                        }
                        Text(event.timeRange(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (showProgressAction) {
                        Button(
                            modifier = Modifier.fillMaxWidth().testTag("${testTag}_progressButton"),
                            onClick = { onProgressClick(event) },
                            enabled = !event.isCompleted,
                        ) {
                            Crossfade(targetState = event.hasStarted, label = "${testTag}_progressButtonText") { hasStarted ->
                                Text(if (hasStarted) "Выполнено" else "За работу")
                            }
                        }
                    }
                    AnimatedVisibility(visible = event.canOpenDetails) {
                        Text("Нажмите на событие, чтобы открыть детали.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CountdownRing(timeText: String, progressPercent: Int, modifier: Modifier = Modifier) {
    val cleanText = timeText.removePrefix("через ").trim()
    val animatedPercent = animateIntAsState(
        targetValue = progressPercent.coerceIn(0, 100),
        label = "countdownProgress",
    )
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.size(72.dp).testTag("nextEventCountdownRing"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(track, style = stroke)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (animatedPercent.value / 100f),
                useCenter = false,
                style = stroke,
                topLeft = Offset(0f, 0f),
                size = size,
            )
        }
        Crossfade(targetState = cleanText, label = "countdownText") { targetText ->
            Text(
                targetText.replace(" мин", "\nмин").replace(" ч ", " ч\n"),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun MealRecommendationCard(meal: MealRecommendationUi) {
    val uriHandler = LocalUriHandler.current
    DashboardCard(modifier = Modifier.testTag("nextMealRecommendation"), elevated = true) {
        Text("Рекомендованная еда", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(meal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        meal.restaurant?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        meal.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            meal.estimatedCalories?.let { AssistChip(onClick = {}, label = { Text("$it ккал") }) }
            meal.estimatedProteinG?.let { AssistChip(onClick = {}, label = { Text("Белок $it г") }) }
            meal.priceRub?.let { AssistChip(onClick = {}, label = { Text("$it ₽") }) }
            meal.weightG?.let { AssistChip(onClick = {}, label = { Text("$it г") }) }
        }
        meal.foodUrl?.let { url ->
            Button(
                modifier = Modifier.fillMaxWidth().testTag("openFoodUrlButton"),
                onClick = { uriHandler.openUri(url) },
            ) {
                Text("Открыть блюдо")
            }
        }
    }
}

@Composable
internal fun DayEventsList(
    events: List<ScheduleEventUi>,
    onEventClick: (ScheduleEventUi) -> Unit,
) {
    if (events.isEmpty()) {
        EmptyStateCard("Нет событий", "На этот день пока нет запланированных действий.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        events.forEach { event ->
            TimelineEventItem(event = event, onEventClick = onEventClick)
        }
    }
}

@Composable
internal fun TimelineEventItem(
    event: ScheduleEventUi,
    onEventClick: (ScheduleEventUi) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timelineEvent_${event.id}"),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { if (event.canOpenDetails) onEventClick(event) },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EventTypeIcon(event.type, event.title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, fontWeight = FontWeight.SemiBold)
                Text(event.timeRange(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                EventStatusChip(event.status)
            }
            if (event.canOpenDetails) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Есть детали",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
            }
            Crossfade(targetState = event.isCompleted, label = "timelineCompletion_${event.id}") { completed ->
                Text(
                    text = if (completed) "Выполнено" else "Не выполнено",
                    color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun CompletedTasksCard(day: DayScheduleUi) {
    val completed = DashboardUiMapper.completedTasks(day)
    DashboardCard {
        Text("Выполнено", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AnimatedVisibility(
            visible = completed.isEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Text("Пока ничего не выполнено.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(
            visible = completed.isNotEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                completed.forEach { event ->
                    FilterChip(selected = true, onClick = {}, label = { Text(event.title) })
                }
            }
        }
    }
}

@Composable
internal fun DailyStatsCard(day: DayScheduleUi) {
    val stats = DashboardUiMapper.dailyStats(day)
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(percent = stats.progressPercent)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("${stats.progressPercent}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${stats.completedCount}/${stats.totalCount} выполнено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Осталось ${stats.remainingCount} - пропущено ${stats.missedCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        stats.categories.forEach { category ->
            MiniProgressRow(category.title, category.progressPercent, "${category.completed}/${category.total}")
        }
    }
}

@Composable
internal fun MiniProgressRow(title: String, progressPercent: Int, value: String) {
    val animatedProgress = animateFloatAsState(
        targetValue = progressPercent.coerceIn(0, 100) / 100f,
        label = "miniProgress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Crossfade(targetState = value, label = "miniProgressValue") { targetValue ->
                Text(targetValue, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(progress = { animatedProgress.value }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun ProgressRing(percent: Int, modifier: Modifier = Modifier) {
    val animatedPercent = animateIntAsState(
        targetValue = percent.coerceIn(0, 100),
        label = "progressRing",
    )
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.size(58.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(track, style = stroke)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (animatedPercent.value / 100f),
                useCenter = false,
                style = stroke,
                topLeft = Offset(0f, 0f),
                size = size,
            )
        }
        Text("${animatedPercent.value}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

internal fun ScheduleEventUi.timeRange(): String = listOfNotNull(startTime, endTime).joinToString(" - ")

private fun ScheduleEventType.drawableRes(): Int = when (this) {
    ScheduleEventType.Sleep -> R.drawable.ic_event_sleep
    ScheduleEventType.Workout -> R.drawable.ic_event_workout
    ScheduleEventType.Posture -> R.drawable.ic_event_posture
    ScheduleEventType.Walk -> R.drawable.ic_event_walk
    ScheduleEventType.Food -> R.drawable.ic_event_food
    ScheduleEventType.Water -> R.drawable.ic_event_water
    ScheduleEventType.Measurement -> R.drawable.ic_event_measurement
    ScheduleEventType.Rest -> R.drawable.ic_event_rest
    ScheduleEventType.Import -> R.drawable.ic_event_import
    ScheduleEventType.Other -> R.drawable.ic_state_empty
}
