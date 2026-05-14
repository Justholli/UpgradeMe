package com.aesthetic.tracker.ui

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.domain.AiAnalysis
import com.aesthetic.tracker.domain.FoodGoal
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanPosition
import com.aesthetic.tracker.domain.ProgressSnapshot
import java.time.LocalDate

enum class TrackerScreen(val label: String) {
    Dashboard("Dashboard"),
    TodayPlan("Today"),
    Measurements("Measurements"),
    Recommendations("Coach"),
    Food("Food"),
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
    val scaleImports: List<ScaleScreenshotImport> = emptyList(),
    val aiAnalysis: AiAnalysis? = null,
    val selectedFoodGoal: FoodGoal = FoodGoal.HighProtein,
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
        val bmi: Double? = null,
        val muscleMassKg: Double? = null,
        val proteinPercent: Double? = null,
        val basalMetabolismKcal: Int? = null,
        val biologicalAge: Int? = null,
        val scalePhotoPath: String? = null,
    ) : AestheticAction
    data class SaveScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class ParseScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class ConfirmParsedScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class DeleteScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class DeleteMeasurement(val measurement: MeasurementEntry) : AestheticAction
    data object GenerateAiAnalysis : AestheticAction
    data class SelectFoodGoal(val goal: FoodGoal) : AestheticAction
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
        val scaleImports: List<ScaleScreenshotImport>,
        val today: LocalDate,
    ) : AestheticMutation
    data class AiAnalysisGenerated(val analysis: AiAnalysis) : AestheticMutation
    data class FoodGoalSelected(val goal: FoodGoal) : AestheticMutation
}

fun reduce(state: AestheticState, mutation: AestheticMutation): AestheticState = when (mutation) {
    is AestheticMutation.ScreenSelected -> state.copy(selectedScreen = mutation.screen)
    is AestheticMutation.AiAnalysisGenerated -> state.copy(aiAnalysis = mutation.analysis)
    is AestheticMutation.FoodGoalSelected -> state.copy(selectedFoodGoal = mutation.goal)
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
            scaleImports = mutation.scaleImports,
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
