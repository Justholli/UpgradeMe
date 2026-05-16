package com.aesthetic.tracker.ui.dashboard

import com.aesthetic.tracker.ui.HabitKind
import java.time.LocalDate

data class DashboardUiState(
    val today: DayScheduleUi,
    val week: List<DayScheduleUi>,
    val progress: ProgressDashboardUi,
) {
    companion object {
        fun empty(date: LocalDate = LocalDate.now()) = DashboardUiState(
            today = DayScheduleUi.empty(date),
            week = emptyList(),
            progress = ProgressDashboardUi.Empty,
        )
    }
}

data class ScheduleEventUi(
    val id: String,
    val title: String,
    val description: String?,
    val type: ScheduleEventType,
    val startTime: String,
    val endTime: String?,
    val status: ScheduleEventStatus,
    val isCompleted: Boolean,
    val timeUntilStartText: String?,
    val timeUntilStartProgressPercent: Int?,
    val eventProgressPercent: Int?,
    val habitKind: HabitKind?,
    val hasStarted: Boolean = false,
    val canBeCompleted: Boolean = habitKind != null,
    val mealRecommendation: MealRecommendationUi? = null,
    val exercises: List<WorkoutExerciseUi> = emptyList(),
) {
    val canOpenDetails: Boolean =
        type == ScheduleEventType.Workout || !description.isNullOrBlank() || mealRecommendation != null || exercises.isNotEmpty()
    val canToggleCompletion: Boolean =
        canBeCompleted && status != ScheduleEventStatus.Upcoming && (hasStarted || isCompleted)
}

data class WorkoutExerciseUi(
    val id: String,
    val exerciseId: String,
    val title: String,
    val description: String?,
    val sets: Int?,
    val reps: String?,
    val durationSec: Int?,
    val restSec: Int?,
    val rpe: String?,
    val equipment: String?,
    val target: List<String>,
    val previewImageUrl: String?,
    val imageUrls: List<String>,
    val imageAlt: String?,
    val sourceUrl: String?,
    val techniqueSteps: List<String>,
    val commonMistakes: List<String>,
)

data class MealRecommendationUi(
    val title: String,
    val restaurant: String?,
    val sourceDescription: String?,
    val description: String?,
    val estimatedCalories: Int?,
    val estimatedProteinG: Int?,
    val estimatedFatG: Int?,
    val estimatedCarbsG: Int?,
    val weightG: Int?,
    val priceRub: Int?,
    val foodUrl: String?,
    val fallback: String?,
)

enum class ScheduleEventType {
    Sleep,
    Workout,
    Posture,
    Walk,
    Food,
    Water,
    Measurement,
    Rest,
    Import,
    Other,
}

enum class ScheduleEventStatus {
    Upcoming,
    Active,
    Completed,
    Missed,
}

enum class DayStatus {
    Today,
    Completed,
    Partial,
    Missed,
    Future,
    Empty,
}

data class DayScheduleUi(
    val date: LocalDate,
    val dateText: String,
    val dayOfWeek: String,
    val events: List<ScheduleEventUi>,
    val progressPercent: Int,
    val completedCount: Int,
    val missedCount: Int,
    val totalCount: Int,
    val remainingCount: Int,
    val description: String?,
    val status: DayStatus,
) {
    companion object {
        fun empty(date: LocalDate) = DayScheduleUi(
            date = date,
            dateText = date.dayOfMonth.toString(),
            dayOfWeek = date.dayOfWeek.name.take(3),
            events = emptyList(),
            progressPercent = 0,
            completedCount = 0,
            missedCount = 0,
            totalCount = 0,
            remainingCount = 0,
            description = null,
            status = DayStatus.Empty,
        )
    }
}

data class DailyStatsUi(
    val completedCount: Int,
    val totalCount: Int,
    val missedCount: Int,
    val remainingCount: Int,
    val progressPercent: Int,
    val categories: List<CategoryProgressUi>,
)

data class CategoryProgressUi(
    val title: String,
    val completed: Int,
    val total: Int,
    val progressPercent: Int,
)

data class ProgressDashboardUi(
    val weeklyProgressPercent: Int,
    val successfulDays: Int,
    val averageCompletionPercent: Int,
    val streakDays: Int,
    val completedEvents: Int,
    val missedEvents: Int,
    val week: List<DayScheduleUi>,
    val categories: List<CategoryProgressUi>,
    val summary: String,
) {
    companion object {
        val Empty = ProgressDashboardUi(
            weeklyProgressPercent = 0,
            successfulDays = 0,
            averageCompletionPercent = 0,
            streakDays = 0,
            completedEvents = 0,
            missedEvents = 0,
            week = emptyList(),
            categories = emptyList(),
            summary = "Импортируйте данные или выполните привычки, чтобы увидеть прогресс.",
        )
    }
}

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    data class Success(val message: String) : ImportUiState
    data class Error(val message: String) : ImportUiState
}
