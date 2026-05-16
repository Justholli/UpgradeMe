package com.aesthetic.tracker.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.ui.AestheticState
import com.aesthetic.tracker.ui.theme.AestheticTrackerTheme
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth

@Composable
fun CalendarScreen(state: AestheticState) {
    val selectedDateText = rememberSaveable { mutableStateOf(state.dashboard.today.date.toString()) }
    val mode = rememberSaveable { mutableStateOf(CalendarMode.Week.name) }
    val selectedDate = LocalDate.parse(selectedDateText.value)
    val selectedMode = CalendarMode.valueOf(mode.value)
    val visibleDays = calendarDays(state, selectedDate, selectedMode)
    val selectedDay = visibleDays.firstOrNull { it.date == selectedDate }
        ?: DashboardUiMapper.buildDay(state, selectedDate)

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen_Calendar"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DashboardCard(elevated = true) {
                Text("Календарь", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Выберите день, чтобы посмотреть события, выполненные действия и статистику.")
                CalendarModeSelector(
                    selected = selectedMode,
                    onSelect = {
                        mode.value = it.name
                        if (it == CalendarMode.Month) selectedDateText.value = state.today.toString()
                    },
                )
                if (selectedMode == CalendarMode.Week) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDateText.value = selectedDate.minusWeeks(1).toString() },
                        ) {
                            Text("Назад")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDateText.value = selectedDate.plusWeeks(1).toString() },
                        ) {
                            Text("Вперед")
                        }
                    }
                }
            }
        }
        item {
            CalendarDays(
                days = visibleDays,
                mode = selectedMode,
                selectedDate = selectedDate,
                onSelect = { selectedDateText.value = it.date.toString() },
            )
        }
        item { SectionTitle("Выбранный день") }
        item {
            DayDetailsContent(
                day = selectedDay,
            )
        }
    }
}

private enum class CalendarMode {
    Week,
    Month,
}

@Composable
private fun CalendarModeSelector(selected: CalendarMode, onSelect: (CalendarMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            modifier = Modifier.testTag("calendarMode_Week"),
            selected = selected == CalendarMode.Week,
            onClick = { onSelect(CalendarMode.Week) },
            label = { Text("Неделя") },
        )
        FilterChip(
            modifier = Modifier.testTag("calendarMode_Month"),
            selected = selected == CalendarMode.Month,
            onClick = { onSelect(CalendarMode.Month) },
            label = { Text("Месяц") },
        )
    }
}

@Composable
private fun CalendarDays(days: List<DayScheduleUi>, mode: CalendarMode, selectedDate: LocalDate, onSelect: (DayScheduleUi) -> Unit) {
    if (mode == CalendarMode.Week) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            days.forEach { day ->
                WeekDayCard(
                    day = day,
                    selected = day.date == selectedDate,
                    onClick = { onSelect(day) },
                    modifier = Modifier.width(118.dp),
                )
            }
        }
    } else {
        MonthCalendar(days = days, selectedDate = selectedDate, onSelect = onSelect)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthCalendar(days: List<DayScheduleUi>, selectedDate: LocalDate, onSelect: (DayScheduleUi) -> Unit) {
    val weeks = days.groupBy { it.date.with(DayOfWeek.MONDAY) }
        .toSortedMap()
        .values
        .map { it.sortedBy(DayScheduleUi::date) }
    val initialPage = weeks.indexOfFirst { week -> week.any { it.date == selectedDate } }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { weeks.size.coerceAtLeast(1) })

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Неделя ${pagerState.currentPage + 1} из ${weeks.size}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks.getOrNull(page).orEmpty().chunked(MONTH_WEEK_CARDS_PER_ROW).forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowDays.forEach { day ->
                            WeekDayCard(
                                day = day,
                                selected = day.date == selectedDate,
                                onClick = { onSelect(day) },
                                modifier = Modifier.weight(1f),
                                compact = true,
                            )
                        }
                        repeat(MONTH_WEEK_CARDS_PER_ROW - rowDays.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private const val MONTH_WEEK_CARDS_PER_ROW = 4

@Composable
internal fun WeekDayCard(
    day: DayScheduleUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Card(
        modifier = modifier
            .testTag("calendarDay_${day.date}")
            .animateContentSize(),
        onClick = onClick,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            Modifier.padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        ) {
            Text(day.dayOfWeek, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                day.dateText,
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!compact) {
                Text("Событий: ${day.totalCount}", style = MaterialTheme.typography.bodySmall)
            }
            Text("${day.progressPercent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!compact) {
                LinearProgressIndicator(progress = { day.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun calendarDays(state: AestheticState, selectedDate: LocalDate, mode: CalendarMode): List<DayScheduleUi> {
    val dates = when (mode) {
        CalendarMode.Week -> {
            val start = selectedDate.with(DayOfWeek.MONDAY)
            (0L..6L).map { start.plusDays(it) }
        }
        CalendarMode.Month -> {
            val month = YearMonth.from(state.today)
            (1..month.lengthOfMonth()).map { day -> month.atDay(day) }
        }
    }
    return dates.map { date ->
        state.dashboard.week.firstOrNull { it.date == date }
            ?: DashboardUiMapper.buildDay(state, date)
    }
}

@Composable
private fun DayDetailsContent(day: DayScheduleUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DailyStatsCard(day)
        CalendarDayEventsSummary(day.events)
        CompletedTasksCard(day)
    }
}

@Composable
private fun CalendarDayEventsSummary(events: List<ScheduleEventUi>) {
    DashboardCard {
        Text("События дня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (events.isEmpty()) {
            Text("На этот день нет событий.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DashboardCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EventTypeIcon(event.type, event.title)
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(event.title, fontWeight = FontWeight.SemiBold)
                        Text(event.timeRange(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (event.isCompleted) "Выполнено" else "Не выполнено",
                            color = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    AestheticTrackerTheme {
        CalendarScreen(
            state = AestheticState(isLoading = false).let { it.copy(dashboard = DashboardUiMapper.map(it)) },
        )
    }
}
