package com.aesthetic.tracker.ui

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanPosition
import com.aesthetic.tracker.domain.ProgressSnapshot
import java.time.LocalDate

enum class TrackerScreen(val label: String) {
    Dashboard("Dashboard"),
    TodayPlan("Today"),
    Measurements("Measurements"),
    Recommendations("Coach"),
}

data class AestheticState(
    val selectedScreen: TrackerScreen = TrackerScreen.Dashboard,
    val planStartDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val measurements: List<MeasurementEntry> = emptyList(),
    val habits: List<HabitEntry> = emptyList(),
    val workoutPlan: List<WorkoutPlanDay> = emptyList(),
    val currentHabit: HabitEntry = HabitEntry(LocalDate.now(), false, false, false, false, false, false),
    val currentPlanDay: WorkoutPlanDay? = null,
    val planPosition: PlanPosition = PlanPosition(1, 1, 0f),
    val progress: ProgressSnapshot = ProgressSnapshot(0.0, 0.0, 0.0, 0, 0.0),
    val recommendations: List<Recommendation> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface AestheticAction {
    data class SelectScreen(val screen: TrackerScreen) : AestheticAction
    data class ToggleHabit(val habit: HabitKind) : AestheticAction
    data class SaveMeasurement(
        val weightKg: Double,
        val bodyFatPercent: Double,
        val skeletalMuscleKg: Double,
        val pulse: Int,
        val visceralFat: Int,
        val waterPercent: Double,
    ) : AestheticAction
}

enum class HabitKind(val label: String) {
    Water("Water"),
    Steps("Steps"),
    Protein("Protein"),
    Workout("Workout"),
    Posture("Posture"),
    Sleep("Sleep"),
}

sealed interface AestheticMutation {
    data class ScreenSelected(val screen: TrackerScreen) : AestheticMutation
    data class DataLoaded(
        val measurements: List<MeasurementEntry>,
        val habits: List<HabitEntry>,
        val workoutPlan: List<WorkoutPlanDay>,
        val today: LocalDate,
    ) : AestheticMutation
}

fun reduce(state: AestheticState, mutation: AestheticMutation): AestheticState = when (mutation) {
    is AestheticMutation.ScreenSelected -> state.copy(selectedScreen = mutation.screen)
    is AestheticMutation.DataLoaded -> {
        val todayHabit = mutation.habits.firstOrNull { it.date == mutation.today }
            ?: HabitEntry(mutation.today, false, false, false, false, false, false)
        val planStart = mutation.measurements.minByOrNull { it.date }?.date ?: state.planStartDate
        val position = com.aesthetic.tracker.domain.currentPlanPosition(planStart, mutation.today)
        val currentPlanDay = mutation.workoutPlan.firstOrNull { it.week == position.week && it.day == position.day }
        val latest = mutation.measurements.firstOrNull()
        state.copy(
            today = mutation.today,
            measurements = mutation.measurements,
            habits = mutation.habits,
            workoutPlan = mutation.workoutPlan,
            currentHabit = todayHabit,
            planStartDate = planStart,
            currentPlanDay = currentPlanDay,
            planPosition = position,
            progress = com.aesthetic.tracker.domain.progressFor(latest, planStart),
            recommendations = com.aesthetic.tracker.domain.buildRecommendations(mutation.measurements, planStart),
            isLoading = false,
        )
    }
}
