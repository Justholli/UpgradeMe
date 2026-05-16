package com.aesthetic.tracker.ui.dashboard

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.ImportedMealRecommendation
import com.aesthetic.tracker.data.ImportedScheduleDay
import com.aesthetic.tracker.data.ImportedScheduleEvent
import com.aesthetic.tracker.data.ImportedWorkoutExercise
import com.aesthetic.tracker.data.ScheduleEventCompletion
import com.aesthetic.tracker.data.ScheduleEventStart
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanPosition
import com.aesthetic.tracker.ui.AestheticState
import com.aesthetic.tracker.ui.HabitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DashboardUiMapperTest {
    private val today = LocalDate.of(2026, 5, 16)
    private val now = LocalDateTime.of(today, LocalTime.of(10, 0))

    @Test
    fun `maps today events with statuses and stats`() {
        val state = stateWithHabit(HabitEntry(today, waterDone = true, stepsDone = false, proteinDone = false, workoutDone = false, postureDone = false, sleepDone = false))

        val day = DashboardUiMapper.buildDay(state, today, now)

        assertEquals(6, day.totalCount)
        assertEquals(1, day.completedCount)
        assertEquals(1, day.missedCount)
        assertEquals(17, day.progressPercent)
        assertEquals(ScheduleEventStatus.Completed, day.events.first { it.type == ScheduleEventType.Water }.status)
        assertEquals(ScheduleEventStatus.Missed, day.events.first { it.type == ScheduleEventType.Walk }.status)
        assertEquals(ScheduleEventStatus.Upcoming, day.events.first { it.type == ScheduleEventType.Food }.status)
    }

    @Test
    fun `picks nearest actionable next event`() {
        val state = stateWithHabit(HabitEntry(today, waterDone = true, stepsDone = false, proteinDone = false, workoutDone = false, postureDone = false, sleepDone = false))

        val next = DashboardUiMapper.nextEvent(DashboardUiMapper.buildDay(state, today, now))

        assertNotNull(next)
        assertEquals(ScheduleEventType.Food, next?.type)
    }

    @Test
    fun `builds exactly seven week days with today marker`() {
        val dashboard = DashboardUiMapper.map(stateWithHabit(HabitEntry(today, false, false, false, false, false, false)), now)

        assertEquals(7, dashboard.week.size)
        assertEquals(1, dashboard.week.count { it.status == DayStatus.Today })
        assertTrue(dashboard.week.any { it.date == today })
    }

    @Test
    fun `uses imported schedule and attaches meal recommendation`() {
        val habit = HabitEntry(today, false, false, false, false, false, false)
        val state = stateWithHabit(habit).copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, "Яндекс Еда", "Челябинск", null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("sleep", today, "02:00", "Сон", "Сон", "Sleep", true, null),
                ImportedScheduleEvent("lunch", today, "15:00", "Обед", "Белковый обед", "Meal", true, null),
            ),
            importedMealRecommendations = listOf(
                ImportedMealRecommendation(
                    id = "meal",
                    date = today,
                    time = "15:00",
                    type = "lunch",
                    title = "Боул с курицей",
                    restaurant = "Оливер",
                    sourceDescription = null,
                    description = "Белковый обед",
                    estimatedCalories = 420,
                    estimatedProteinG = 32,
                    estimatedFatG = null,
                    estimatedCarbsG = null,
                    weightG = 290,
                    priceRub = 660,
                    foodUrl = "https://example.com",
                    source = "site",
                    fallback = "Взять курицу с овощами",
                ),
            ),
        )

        val day = DashboardUiMapper.buildDay(state, today, LocalDateTime.of(today, LocalTime.of(14, 0)))
        val next = DashboardUiMapper.nextEvent(day)

        assertEquals(2, day.totalCount)
        assertEquals(ScheduleEventType.Food, next?.type)
        assertEquals("Боул с курицей", next?.mealRecommendation?.title)
        assertEquals("https://example.com", next?.mealRecommendation?.foodUrl)
    }

    @Test
    fun `maps imported workout exercises to schedule event ui`() {
        val habit = HabitEntry(today, false, false, false, false, false, false)
        val state = stateWithHabit(habit).copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, null, null, null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("workout", today, "18:00", "Upper body", null, "Workout", false, null),
            ),
            importedWorkoutExercises = listOf(
                ImportedWorkoutExercise(
                    id = "exercise-1",
                    date = today,
                    eventId = "workout",
                    workoutId = "upper-body",
                    workoutTitle = "Upper body",
                    workoutDescription = "Домашняя тренировка",
                    workoutEstimatedDurationMin = 30,
                    workoutIntensity = "light",
                    exerciseId = "wall-slides",
                    orderIndex = 0,
                    title = "Wall slides",
                    description = "4x10-12",
                    sets = 4,
                    reps = "10-12",
                    durationSec = null,
                    restSec = 30,
                    rpe = "5",
                    equipment = "none",
                    target = listOf("плечи"),
                    previewImageUrl = "https://example.com/wall.png",
                    imageUrls = listOf("https://example.com/wall-1.png"),
                    imageAlt = "Wall slides",
                    sourceUrl = null,
                    techniqueSteps = listOf("Ребра вниз"),
                    commonMistakes = listOf("Не прогибать поясницу"),
                ),
            ),
        )

        val day = DashboardUiMapper.buildDay(state, today, LocalDateTime.of(today, LocalTime.of(17, 0)))
        val workout = day.events.single()

        assertEquals(ScheduleEventType.Workout, workout.type)
        assertEquals(HabitKind.Workout, workout.habitKind)
        assertTrue(workout.canOpenDetails)
        assertFalse(workout.canToggleCompletion)
        assertEquals("Домашняя тренировка", workout.description)
        assertEquals("Wall slides", workout.exercises.single().title)
        assertEquals("https://example.com/wall.png", workout.exercises.single().previewImageUrl)
    }

    @Test
    fun `only started events can be marked completed`() {
        val habit = HabitEntry(today, false, false, false, false, false, false)
        val state = stateWithHabit(habit).copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, null, null, null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("checkin", today, "09:00", "Check-in", null, "CheckIn", false, null),
                ImportedScheduleEvent("future-workout", today, "18:00", "Future workout", null, "Workout", false, null),
                ImportedScheduleEvent("active-workout", today, "10:00", "Active workout", null, "Workout", false, null),
            ),
            scheduleEventStarts = listOf(ScheduleEventStart("active-workout", today)),
        )

        val day = DashboardUiMapper.buildDay(state, today, LocalDateTime.of(today, LocalTime.of(10, 5)))

        val checkIn = day.events.first { it.id == "checkin" }
        assertEquals(HabitKind.CheckIn, checkIn.habitKind)
        assertFalse(checkIn.canToggleCompletion)
        assertFalse(day.events.first { it.id == "future-workout" }.canToggleCompletion)
        assertTrue(day.events.first { it.id == "active-workout" }.canToggleCompletion)
    }

    @Test
    fun `imported event reflects per event completion state`() {
        val habit = HabitEntry(today, false, false, false, false, false, false)
        val state = stateWithHabit(habit).copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, null, null, null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("checkin", today, "09:00", "Check-in", null, "CheckIn", false, null),
            ),
            scheduleEventCompletions = listOf(ScheduleEventCompletion("checkin", today)),
        )

        val checkIn = DashboardUiMapper.buildDay(state, today, LocalDateTime.of(today, LocalTime.of(10, 5)))
            .events
            .single()

        assertEquals(ScheduleEventType.Measurement, checkIn.type)
        assertEquals(HabitKind.CheckIn, checkIn.habitKind)
        assertTrue(checkIn.isCompleted)
        assertTrue(checkIn.canToggleCompletion)
    }

    @Test
    fun `imported meal events complete independently`() {
        val state = stateWithHabit(HabitEntry(today, false, false, false, false, false, false)).copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, null, null, null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("lunch", today, "12:00", "Обед", null, "Meal", false, null),
                ImportedScheduleEvent("dinner", today, "19:00", "Ужин", null, "Meal", false, null),
            ),
            scheduleEventStarts = listOf(ScheduleEventStart("lunch", today), ScheduleEventStart("dinner", today)),
            scheduleEventCompletions = listOf(ScheduleEventCompletion("lunch", today)),
        )

        val events = DashboardUiMapper.buildDay(state, today, LocalDateTime.of(today, LocalTime.of(20, 0))).events

        assertTrue(events.first { it.id == "lunch" }.isCompleted)
        assertFalse(events.first { it.id == "dinner" }.isCompleted)
        assertTrue(events.first { it.id == "dinner" }.canToggleCompletion)
    }

    private fun stateWithHabit(habit: HabitEntry): AestheticState = AestheticState(
        today = today,
        habits = listOf(habit),
        currentHabit = habit,
        workoutPlan = listOf(WorkoutPlanDay(1, 1, "Upper body", listOf("Push-ups", "Rows"), "Strength")),
        currentPlanDay = WorkoutPlanDay(1, 1, "Upper body", listOf("Push-ups", "Rows"), "Strength"),
        planPosition = PlanPosition(1, 1, 0f),
        isLoading = false,
    )
}
