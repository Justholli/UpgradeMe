package com.aesthetic.tracker.ui

import androidx.annotation.StringRes
import com.aesthetic.tracker.R
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.domain.FoodGoal
import com.aesthetic.tracker.domain.GeneratedTodayPlan
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanPosition
import com.aesthetic.tracker.domain.ProgressSnapshot
import com.aesthetic.tracker.domain.TodayPlanInput
import com.aesthetic.tracker.domain.signature
import java.time.LocalDate

enum class TrackerScreen(@StringRes val labelRes: Int) {
    GeneralData(R.string.screen_general_data),
    Today(R.string.screen_today),
    UploadResults(R.string.screen_upload_results),
}

data class AestheticState(
    val selectedScreen: TrackerScreen = TrackerScreen.GeneralData,
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
    val generatedTodayPlan: GeneratedTodayPlan? = null,
    val completedScheduleItemIds: Set<String> = emptySet(),
    val chatUrl: String = "",
    val todayPlanError: String? = null,
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
        val bodyScore: Int? = null,
        val fatMassKg: Double? = null,
        val muscleMassKg: Double? = null,
        val muscleRatePercent: Double? = null,
        val bodyWaterKg: Double? = null,
        val bmi: Double? = null,
        val mineralMassKg: Double? = null,
        val proteinMassKg: Double? = null,
        val proteinPercent: Double? = null,
        val subcutaneousFatPercent: Double? = null,
        val leanBodyMassKg: Double? = null,
        val basalMetabolismKcal: Int? = null,
        val biologicalAge: Int? = null,
        val bodyType: String? = null,
        val standardWeightKg: Double? = null,
        val weightControlKg: Double? = null,
        val fatControlKg: Double? = null,
        val muscleControlKg: Double? = null,
        val scalePhotoPath: String? = null,
    ) : AestheticAction
    data class SaveScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class ParseScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class ConfirmParsedScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class DeleteScaleImport(val scaleImport: ScaleScreenshotImport) : AestheticAction
    data class DeleteMeasurement(val measurement: MeasurementEntry) : AestheticAction
    data class ImportTodayPlan(val content: String) : AestheticAction
    data class ToggleScheduleItem(val itemId: String) : AestheticAction
    data class SaveChatUrl(val url: String) : AestheticAction
    data class SelectFoodGoal(val goal: FoodGoal) : AestheticAction
}

enum class HabitKind(@StringRes val labelRes: Int) {
    Water(R.string.habit_water),
    Steps(R.string.habit_steps),
    Protein(R.string.habit_protein),
    Workout(R.string.habit_workout),
    Posture(R.string.habit_posture),
    Sleep(R.string.habit_sleep),
}

sealed interface AestheticMutation {
    data class ScreenSelected(val screen: TrackerScreen) : AestheticMutation
    data class DataLoaded(
        val measurements: List<MeasurementEntry>,
        val habits: List<HabitEntry>,
        val workoutPlan: List<WorkoutPlanDay>,
        val scaleImports: List<ScaleScreenshotImport>,
        val chatUrl: String,
        val today: LocalDate,
    ) : AestheticMutation
    data class TodayPlanGenerated(val plan: GeneratedTodayPlan) : AestheticMutation
    data class TodayPlanGenerationFailed(val message: String) : AestheticMutation
    data class FoodGoalSelected(val goal: FoodGoal) : AestheticMutation
    data class ScheduleItemToggled(val itemId: String) : AestheticMutation
    data class ChatUrlSaved(val url: String) : AestheticMutation
}

fun reduce(state: AestheticState, mutation: AestheticMutation): AestheticState = when (mutation) {
    is AestheticMutation.ScreenSelected -> state.copy(selectedScreen = mutation.screen)
    is AestheticMutation.TodayPlanGenerated -> state.copy(
        generatedTodayPlan = mutation.plan,
        todayPlanError = null,
    )
    is AestheticMutation.TodayPlanGenerationFailed -> state.copy(todayPlanError = mutation.message)
    is AestheticMutation.FoodGoalSelected -> state.copy(selectedFoodGoal = mutation.goal)
        .invalidateStaleTodayPlan()
    is AestheticMutation.ScheduleItemToggled -> {
        val nextIds = if (mutation.itemId in state.completedScheduleItemIds) {
            state.completedScheduleItemIds - mutation.itemId
        } else {
            state.completedScheduleItemIds + mutation.itemId
        }
        state.copy(completedScheduleItemIds = nextIds)
    }
    is AestheticMutation.ChatUrlSaved -> state.copy(chatUrl = mutation.url)
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
            completedScheduleItemIds = if (mutation.today == state.today) state.completedScheduleItemIds else emptySet(),
            chatUrl = mutation.chatUrl,
            isLoading = false,
        ).invalidateStaleTodayPlan()
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
