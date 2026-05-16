package com.aesthetic.tracker.ui.dashboard

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.ImportedMealRecommendation
import com.aesthetic.tracker.data.ImportedScheduleEvent
import com.aesthetic.tracker.data.ImportedWorkoutExercise
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanTargets
import com.aesthetic.tracker.ui.AestheticState
import com.aesthetic.tracker.ui.HabitKind
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

object DashboardUiMapper {
    fun map(state: AestheticState, now: LocalDateTime = LocalDateTime.now()): DashboardUiState {
        val week = buildWeek(state, now)
        val today = week.firstOrNull { it.date == state.today } ?: buildDay(state, state.today, now)
        return DashboardUiState(
            today = today,
            week = week,
            progress = buildProgress(week, state.measurements),
        )
    }

    fun buildDay(state: AestheticState, date: LocalDate, now: LocalDateTime = LocalDateTime.now()): DayScheduleUi {
        val habit = state.habits.firstOrNull { it.date == date }
            ?: if (date == state.today) state.currentHabit else HabitEntry(date, false, false, false, false, false, false)
        val planDay = planDayFor(date, state)
        val importedDay = state.importedScheduleDays.firstOrNull { it.date == date }
        val events = buildEvents(date = date, today = state.today, now = now, habit = habit, planDay = planDay, state = state)
        val completed = events.count { it.status == ScheduleEventStatus.Completed }
        val missed = events.count { it.status == ScheduleEventStatus.Missed }
        val total = events.size
        val progress = percent(completed, total)
        return DayScheduleUi(
            date = date,
            dateText = date.dayOfMonth.toString(),
            dayOfWeek = date.dayOfWeek.shortName(),
            events = events,
            progressPercent = progress,
            completedCount = completed,
            missedCount = missed,
            totalCount = total,
            remainingCount = (total - completed - missed).coerceAtLeast(0),
            description = importedDay?.focus ?: planDay?.focus,
            status = dayStatus(date, state.today, total, completed, missed),
        )
    }

    fun currentEvent(day: DayScheduleUi): ScheduleEventUi? =
        day.events.firstOrNull { it.status == ScheduleEventStatus.Active }

    fun nextUpcomingEvent(day: DayScheduleUi): ScheduleEventUi? =
        day.events.firstOrNull { it.status == ScheduleEventStatus.Upcoming }

    fun dailyStats(day: DayScheduleUi): DailyStatsUi = DailyStatsUi(
        completedCount = day.completedCount,
        totalCount = day.totalCount,
        missedCount = day.missedCount,
        remainingCount = day.remainingCount,
        progressPercent = day.progressPercent,
        categories = categoryProgress(day.events),
    )

    fun completedTasks(day: DayScheduleUi): List<ScheduleEventUi> =
        day.events.filter { it.status == ScheduleEventStatus.Completed }

    fun nextEvent(day: DayScheduleUi): ScheduleEventUi? =
        currentEvent(day) ?: nextUpcomingEvent(day)

    private fun buildWeek(state: AestheticState, now: LocalDateTime): List<DayScheduleUi> {
        val start = state.today.with(DayOfWeek.MONDAY)
        return (0L..6L).map { offset -> buildDay(state, start.plusDays(offset), now) }
    }

    private fun buildProgress(week: List<DayScheduleUi>, measurements: List<MeasurementEntry>): ProgressDashboardUi {
        if (week.isEmpty()) return ProgressDashboardUi.Empty
        val total = week.sumOf { it.totalCount }
        val completed = week.sumOf { it.completedCount }
        val missed = week.sumOf { it.missedCount }
        val successfulDays = week.count { it.totalCount > 0 && it.progressPercent == 100 }
        val average = week.map { it.progressPercent }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
        val streak = week
            .takeWhile { it.status != DayStatus.Future }
            .reversed()
            .takeWhile { it.totalCount > 0 && it.progressPercent == 100 }
            .count()
        val weightSummary = measurements.firstOrNull()?.let { latest ->
            "Последний замер: ${latest.weightKg} кг, жир ${latest.bodyFatPercent}%."
        } ?: "Замеров пока нет. Импортируйте данные или добавьте чек-ин, чтобы увидеть тренды."
        return ProgressDashboardUi(
            weeklyProgressPercent = percent(completed, total),
            successfulDays = successfulDays,
            averageCompletionPercent = average.coerceIn(0, 100),
            streakDays = streak,
            completedEvents = completed,
            missedEvents = missed,
            week = week,
            categories = categoryProgress(week.flatMap { it.events }),
            summary = weightSummary,
        )
    }

    private fun buildEvents(
        date: LocalDate,
        today: LocalDate,
        now: LocalDateTime,
        habit: HabitEntry,
        planDay: WorkoutPlanDay?,
        state: AestheticState,
    ): List<ScheduleEventUi> {
        val importedEvents = state.importedScheduleEvents.filter { it.date == date }
        val completedEventIds = state.scheduleEventCompletions.mapTo(mutableSetOf()) { it.eventId }
        val startedEventIds = state.scheduleEventStarts.mapTo(mutableSetOf()) { it.eventId }
        if (importedEvents.isNotEmpty()) {
            return importedEvents
                .sortedBy { it.time.asLocalTimeOrNull() ?: LocalTime.MAX }
                .map { imported ->
                    imported.toScheduleEventUi(
                        today = today,
                        now = now,
                        meal = state.importedMealRecommendations.findFor(imported),
                        exercises = state.importedWorkoutExercises.findFor(imported),
                        completedEventIds = completedEventIds,
                        startedEventIds = startedEventIds,
                    )
                }
        }
        val workoutTitle = planDay?.title ?: "Workout"
        return listOf(
            EventSeed("water", "Вода", "Начните пить воду заранее и держите бутылку рядом.", ScheduleEventType.Water, LocalTime.of(8, 0), LocalTime.of(8, 15), HabitKind.Water, habit.waterDone),
            EventSeed("walk", "Утренняя прогулка", "Легкая прогулка поддерживает восстановление и пульс покоя.", ScheduleEventType.Walk, LocalTime.of(9, 0), LocalTime.of(9, 30), HabitKind.Steps, habit.stepsDone),
            EventSeed("food", "Белковый прием пищи", "Закройте цель по белку на сегодня.", ScheduleEventType.Food, LocalTime.of(13, 0), LocalTime.of(13, 30), HabitKind.Protein, habit.proteinDone),
            EventSeed("posture", "Осанка", "Короткая мобилизация и работа над осанкой между рабочими блоками.", ScheduleEventType.Posture, LocalTime.of(15, 0), LocalTime.of(15, 10), HabitKind.Posture, habit.postureDone),
            EventSeed("workout", workoutTitle, planDay?.exercises?.joinToString(separator = "\n") ?: "Силовая тренировка из текущего плана.", ScheduleEventType.Workout, LocalTime.of(18, 0), LocalTime.of(19, 0), HabitKind.Workout, habit.workoutDone),
            EventSeed("sleep", "Подготовка ко сну", "Снизьте нагрузку, уберите экраны и подготовьтесь к качественному сну.", ScheduleEventType.Sleep, LocalTime.of(22, 30), LocalTime.of(23, 0), HabitKind.Sleep, habit.sleepDone),
        ).map { seed ->
            val start = LocalDateTime.of(date, seed.start)
            val end = LocalDateTime.of(date, seed.end)
            ScheduleEventUi(
                id = "${date}-${seed.id}",
                title = seed.title,
                description = seed.description,
                type = seed.type,
                startTime = seed.start.toString(),
                endTime = seed.end.toString(),
                status = eventStatus(date, today, now, start, end, seed.completed),
                isCompleted = seed.completed,
                timeUntilStartText = timeUntil(now, start, date, today, seed.completed),
                timeUntilStartProgressPercent = timeUntilProgress(now, start, date, today, seed.completed),
                eventProgressPercent = activeProgress(now, start, end, date, today, seed.completed),
                habitKind = seed.habitKind,
                hasStarted = seed.completed || startedEventIds.contains("${date}-${seed.id}"),
                canBeCompleted = true,
                exercises = emptyList(),
            )
        }
    }

    private fun ImportedScheduleEvent.toScheduleEventUi(
        today: LocalDate,
        now: LocalDateTime,
        meal: ImportedMealRecommendation?,
        exercises: List<ImportedWorkoutExercise>,
        completedEventIds: Set<String>,
        startedEventIds: Set<String>,
    ): ScheduleEventUi {
        val startTime = time.asLocalTimeOrNull() ?: LocalTime.MIDNIGHT
        val endTime = startTime.plusMinutes(defaultDurationMinutes(kind))
        val habitKind = kind.asHabitKind()
        val completed = completedEventIds.contains(id)
        val start = LocalDateTime.of(date, startTime)
        val end = LocalDateTime.of(date, endTime)
        return ScheduleEventUi(
            id = id,
            title = title,
            description = description ?: notificationText ?: exercises.firstNotNullOfOrNull { it.workoutDescription },
            type = kind.asScheduleEventType(),
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            status = eventStatus(date, today, now, start, end, completed),
            isCompleted = completed,
            timeUntilStartText = timeUntil(now, start, date, today, completed),
            timeUntilStartProgressPercent = timeUntilProgress(now, start, date, today, completed),
            eventProgressPercent = activeProgress(now, start, end, date, today, completed),
            habitKind = habitKind,
            hasStarted = startedEventIds.contains(id),
            canBeCompleted = true,
            mealRecommendation = meal?.toUi(),
            exercises = exercises.map { it.toUi() },
        )
    }

    private fun eventStatus(
        date: LocalDate,
        today: LocalDate,
        now: LocalDateTime,
        start: LocalDateTime,
        end: LocalDateTime,
        completed: Boolean,
    ): ScheduleEventStatus = when {
        completed -> ScheduleEventStatus.Completed
        date.isAfter(today) -> ScheduleEventStatus.Upcoming
        date.isBefore(today) -> ScheduleEventStatus.Missed
        now.isBefore(start) -> ScheduleEventStatus.Upcoming
        now.isAfter(end) -> ScheduleEventStatus.Missed
        else -> ScheduleEventStatus.Active
    }

    private fun timeUntil(now: LocalDateTime, start: LocalDateTime, date: LocalDate, today: LocalDate, completed: Boolean): String? {
        if (completed || date != today || !now.isBefore(start)) return null
        val duration = Duration.between(now, start)
        val hours = duration.toHours()
        val minutes = duration.minusHours(hours).toMinutes()
        return when {
            hours > 0 -> "$hours ч $minutes мин"
            minutes > 0 -> "$minutes мин"
            else -> "скоро"
        }
    }

    private fun timeUntilProgress(now: LocalDateTime, start: LocalDateTime, date: LocalDate, today: LocalDate, completed: Boolean): Int? {
        if (completed || date != today || !now.isBefore(start)) return null
        val minutesUntil = Duration.between(now, start).toMinutes().coerceAtLeast(0)
        val countdownWindowMinutes = 240L
        val elapsedInWindow = (countdownWindowMinutes - minutesUntil).coerceIn(0, countdownWindowMinutes)
        return percent(elapsedInWindow.toInt(), countdownWindowMinutes.toInt())
    }

    private fun activeProgress(now: LocalDateTime, start: LocalDateTime, end: LocalDateTime, date: LocalDate, today: LocalDate, completed: Boolean): Int? {
        if (completed || date != today || now.isBefore(start) || now.isAfter(end)) return null
        val total = Duration.between(start, end).toMinutes().coerceAtLeast(1)
        val elapsed = Duration.between(start, now).toMinutes().coerceIn(0, total)
        return percent(elapsed.toInt(), total.toInt())
    }

    private fun planDayFor(date: LocalDate, state: AestheticState): WorkoutPlanDay? {
        val daysFromToday = java.time.temporal.ChronoUnit.DAYS.between(state.today, date).toInt()
        val day = (state.planPosition.day + daysFromToday - 1).floorMod(7) + 1
        val weekOffset = (state.planPosition.day + daysFromToday - 1).floorDiv(7)
        val week = (state.planPosition.week + weekOffset).coerceIn(1, PlanTargets.PlanLengthWeeks)
        return state.workoutPlan.firstOrNull { it.week == week && it.day == day } ?: state.currentPlanDay
    }

    private fun dayStatus(date: LocalDate, today: LocalDate, total: Int, completed: Int, missed: Int): DayStatus = when {
        total == 0 -> DayStatus.Empty
        date == today -> DayStatus.Today
        date.isAfter(today) -> DayStatus.Future
        completed == total -> DayStatus.Completed
        completed > 0 -> DayStatus.Partial
        missed > 0 -> DayStatus.Missed
        else -> DayStatus.Empty
    }

    private fun categoryProgress(events: List<ScheduleEventUi>): List<CategoryProgressUi> =
        events.groupBy { it.type }.map { (type, items) ->
            CategoryProgressUi(
                title = type.title(),
                completed = items.count { it.status == ScheduleEventStatus.Completed },
                total = items.size,
                progressPercent = percent(items.count { it.status == ScheduleEventStatus.Completed }, items.size),
            )
        }

    private fun ScheduleEventType.title(): String = when (this) {
        ScheduleEventType.Sleep -> "Сон"
        ScheduleEventType.Workout -> "Тренировка"
        ScheduleEventType.Posture -> "Осанка"
        ScheduleEventType.Walk -> "Прогулка"
        ScheduleEventType.Food -> "Питание"
        ScheduleEventType.Water -> "Вода"
        ScheduleEventType.Measurement -> "Замер"
        ScheduleEventType.Rest -> "Отдых"
        ScheduleEventType.Import -> "Импорт"
        ScheduleEventType.Other -> "Другое"
    }

    private fun percent(completed: Int, total: Int): Int =
        if (total <= 0) 0 else ((completed.toFloat() / total) * 100).roundToInt().coerceIn(0, 100)

    private fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

    private fun Int.floorMod(other: Int): Int = Math.floorMod(this, other)

    private fun Int.floorDiv(other: Int): Int = Math.floorDiv(this, other)

    private fun String.asLocalTimeOrNull(): LocalTime? =
        runCatching { LocalTime.parse(this) }.getOrNull()

    private fun String.asScheduleEventType(): ScheduleEventType = when (lowercase(Locale.ROOT)) {
        "sleep" -> ScheduleEventType.Sleep
        "workout" -> ScheduleEventType.Workout
        "meal" -> ScheduleEventType.Food
        "habit" -> ScheduleEventType.Water
        "recovery" -> ScheduleEventType.Walk
        "checkin" -> ScheduleEventType.Measurement
        else -> ScheduleEventType.Other
    }

    private fun String.asHabitKind(): HabitKind? = when (lowercase(Locale.ROOT)) {
        "sleep" -> HabitKind.Sleep
        "workout" -> HabitKind.Workout
        "meal" -> HabitKind.Protein
        "habit" -> HabitKind.Water
        "recovery" -> HabitKind.Steps
        "checkin" -> HabitKind.CheckIn
        else -> null
    }

    private fun defaultDurationMinutes(kind: String): Long = when (kind.lowercase(Locale.ROOT)) {
        "sleep" -> 480
        "workout" -> 60
        "meal" -> 45
        "recovery" -> 45
        "checkin" -> 15
        else -> 20
    }

    private fun List<ImportedMealRecommendation>.findFor(event: ImportedScheduleEvent): ImportedMealRecommendation? =
        firstOrNull { it.date == event.date && it.time == event.time }
            ?: firstOrNull { it.date == event.date && it.type.equals(event.kind, ignoreCase = true) }

    private fun List<ImportedWorkoutExercise>.findFor(event: ImportedScheduleEvent): List<ImportedWorkoutExercise> =
        filter { exercise ->
            exercise.date == event.date && (
                exercise.eventId == event.id ||
                    (!exercise.workoutTitle.isNullOrBlank() && exercise.workoutTitle == event.title)
                )
        }.sortedBy { it.orderIndex }

    private fun ImportedMealRecommendation.toUi(): MealRecommendationUi = MealRecommendationUi(
        title = title,
        restaurant = restaurant,
        sourceDescription = sourceDescription,
        description = description,
        estimatedCalories = estimatedCalories,
        estimatedProteinG = estimatedProteinG,
        estimatedFatG = estimatedFatG,
        estimatedCarbsG = estimatedCarbsG,
        weightG = weightG,
        priceRub = priceRub,
        foodUrl = foodUrl,
        fallback = fallback,
    )

    private fun ImportedWorkoutExercise.toUi(): WorkoutExerciseUi = WorkoutExerciseUi(
        id = id,
        exerciseId = exerciseId,
        title = title,
        description = description,
        sets = sets,
        reps = reps,
        durationSec = durationSec,
        restSec = restSec,
        rpe = rpe,
        equipment = equipment,
        target = target,
        previewImageUrl = previewImageUrl,
        imageUrls = imageUrls,
        imageAlt = imageAlt,
        sourceUrl = sourceUrl,
        techniqueSteps = techniqueSteps,
        commonMistakes = commonMistakes,
    )
}

private data class EventSeed(
    val id: String,
    val title: String,
    val description: String,
    val type: ScheduleEventType,
    val start: LocalTime,
    val end: LocalTime,
    val habitKind: HabitKind,
    val completed: Boolean,
)
