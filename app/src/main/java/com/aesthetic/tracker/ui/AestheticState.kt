package com.aesthetic.tracker.ui

import androidx.annotation.StringRes
import com.aesthetic.tracker.R
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.ImportedGoal
import com.aesthetic.tracker.data.ImportedMealRecommendation
import com.aesthetic.tracker.data.ImportedScheduleDay
import com.aesthetic.tracker.data.ImportedScheduleEvent
import com.aesthetic.tracker.data.ImportedWorkoutExercise
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.ScheduleEventCompletion
import com.aesthetic.tracker.data.ScheduleEventStart
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.domain.FoodGoal
import com.aesthetic.tracker.domain.GeneratedTodayPlan
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanPosition
import com.aesthetic.tracker.domain.ProgressSnapshot
import com.aesthetic.tracker.domain.TodayPlanInput
import com.aesthetic.tracker.domain.signature
import com.aesthetic.tracker.ui.dashboard.DashboardUiMapper
import com.aesthetic.tracker.ui.dashboard.DashboardUiState
import com.aesthetic.tracker.ui.dashboard.ImportUiState
import java.time.LocalDate

enum class TrackerScreen(@StringRes val labelRes: Int) {
    Today(R.string.screen_today),
    Calendar(R.string.screen_calendar),
    Progress(R.string.screen_progress),
}

data class AestheticState(
    val selectedScreen: TrackerScreen = TrackerScreen.Today,
    val planStartDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val measurements: List<MeasurementEntry> = emptyList(),
    val habits: List<HabitEntry> = emptyList(),
    val workoutPlan: List<WorkoutPlanDay> = emptyList(),
    val importedScheduleDays: List<ImportedScheduleDay> = emptyList(),
    val importedScheduleEvents: List<ImportedScheduleEvent> = emptyList(),
    val scheduleEventCompletions: List<ScheduleEventCompletion> = emptyList(),
    val scheduleEventStarts: List<ScheduleEventStart> = emptyList(),
    val importedMealRecommendations: List<ImportedMealRecommendation> = emptyList(),
    val importedWorkoutExercises: List<ImportedWorkoutExercise> = emptyList(),
    val importedGoal: ImportedGoal? = null,
    val currentHabit: HabitEntry = HabitEntry(LocalDate.now(), false, false, false, false, false, false),
    val currentPlanDay: WorkoutPlanDay? = null,
    val planPosition: PlanPosition = PlanPosition(1, 1, 0f),
    val progress: ProgressSnapshot = ProgressSnapshot(0.0, 0.0, 0.0, 0, 0.0),
    val recommendations: List<Recommendation> = emptyList(),
    val scaleImports: List<ScaleScreenshotImport> = emptyList(),
    val generatedTodayPlan: GeneratedTodayPlan? = null,
    val selectedFoodGoal: FoodGoal = FoodGoal.HighProtein,
    val dashboard: DashboardUiState = DashboardUiState.empty(),
    val importUiState: ImportUiState = ImportUiState.Idle,
    val isLoading: Boolean = true,
)

sealed interface AestheticAction {
    data class SelectScreen(val screen: TrackerScreen) : AestheticAction
    data class ToggleHabit(val habit: HabitKind) : AestheticAction
    data class ToggleScheduleEventCompletion(val eventId: String) : AestheticAction
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
    data object GenerateTodayPlan : AestheticAction
    data class SelectFoodGoal(val goal: FoodGoal) : AestheticAction
    data class SubmitJsonImport(val json: String) : AestheticAction
    data class StartEventProgressNotification(val eventId: String, val title: String, val progressPercent: Int) : AestheticAction
    data object DismissImportStatus : AestheticAction
    data object ClearImportedSchedule : AestheticAction
}

enum class HabitKind {
    Water,
    Steps,
    Protein,
    Workout,
    Posture,
    Sleep,
    CheckIn,
}

sealed interface AestheticMutation {
    data class ScreenSelected(val screen: TrackerScreen) : AestheticMutation
    data class DataLoaded(
        val measurements: List<MeasurementEntry>,
        val habits: List<HabitEntry>,
        val workoutPlan: List<WorkoutPlanDay>,
        val scaleImports: List<ScaleScreenshotImport>,
        val importedScheduleDays: List<ImportedScheduleDay>,
        val importedScheduleEvents: List<ImportedScheduleEvent>,
        val scheduleEventCompletions: List<ScheduleEventCompletion>,
        val scheduleEventStarts: List<ScheduleEventStart>,
        val importedMealRecommendations: List<ImportedMealRecommendation>,
        val importedWorkoutExercises: List<ImportedWorkoutExercise>,
        val importedGoal: ImportedGoal?,
        val today: LocalDate,
    ) : AestheticMutation
    data class TodayPlanGenerated(val plan: GeneratedTodayPlan) : AestheticMutation
    data class FoodGoalSelected(val goal: FoodGoal) : AestheticMutation
    data object ImportStarted : AestheticMutation
    data class ImportSucceeded(val message: String) : AestheticMutation
    data class ImportFailed(val message: String) : AestheticMutation
    data object ImportDismissed : AestheticMutation
}

fun reduce(state: AestheticState, mutation: AestheticMutation): AestheticState = when (mutation) {
    is AestheticMutation.ScreenSelected -> state.copy(selectedScreen = mutation.screen)
    is AestheticMutation.TodayPlanGenerated -> state.copy(generatedTodayPlan = mutation.plan)
    is AestheticMutation.FoodGoalSelected -> state.copy(selectedFoodGoal = mutation.goal).invalidateStaleTodayPlan()
    is AestheticMutation.ImportStarted -> state.copy(importUiState = ImportUiState.Loading)
    is AestheticMutation.ImportSucceeded -> state.copy(importUiState = ImportUiState.Success(mutation.message))
    is AestheticMutation.ImportFailed -> state.copy(importUiState = ImportUiState.Error(mutation.message))
    is AestheticMutation.ImportDismissed -> state.copy(importUiState = ImportUiState.Idle)
    is AestheticMutation.DataLoaded -> {
        val todayHabit = mutation.habits.firstOrNull { it.date == mutation.today }
            ?: HabitEntry(mutation.today, false, false, false, false, false, false)
        val planStart = mutation.measurements.minByOrNull { it.date }?.date ?: state.planStartDate
        val position = com.aesthetic.tracker.domain.currentPlanPosition(planStart, mutation.today)
        val currentPlanDay = mutation.workoutPlan.firstOrNull { it.week == position.week && it.day == position.day }
        val latest = mutation.measurements.firstOrNull()
        val nextState = state.copy(
            today = mutation.today,
            measurements = mutation.measurements,
            habits = mutation.habits,
            workoutPlan = mutation.workoutPlan,
            importedScheduleDays = mutation.importedScheduleDays,
            importedScheduleEvents = mutation.importedScheduleEvents,
            scheduleEventCompletions = mutation.scheduleEventCompletions,
            scheduleEventStarts = mutation.scheduleEventStarts,
            importedMealRecommendations = mutation.importedMealRecommendations,
            importedWorkoutExercises = mutation.importedWorkoutExercises,
            importedGoal = mutation.importedGoal,
            scaleImports = mutation.scaleImports,
            currentHabit = todayHabit,
            planStartDate = planStart,
            currentPlanDay = currentPlanDay,
            planPosition = position,
            progress = com.aesthetic.tracker.domain.progressFor(latest, planStart),
            recommendations = com.aesthetic.tracker.domain.buildRecommendations(mutation.measurements, planStart),
            isLoading = false,
        ).invalidateStaleTodayPlan()
        nextState.copy(dashboard = DashboardUiMapper.map(nextState))
    }
}

fun AestheticState.todayPlanInput(): TodayPlanInput = TodayPlanInput(
    today = today,
    measurementsDescending = measurements,
    currentPlanDay = currentPlanDay,
    planPosition = planPosition,
    currentHabit = currentHabit,
    selectedFoodGoal = selectedFoodGoal,
)

private fun AestheticState.invalidateStaleTodayPlan(): AestheticState {
    val plan = generatedTodayPlan ?: return this
    return if (plan.signature == todayPlanInput().signature()) this else copy(generatedTodayPlan = null)
}
