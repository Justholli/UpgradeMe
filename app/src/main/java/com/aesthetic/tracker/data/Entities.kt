package com.aesthetic.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "measurement_entries")
data class MeasurementEntry(
    @PrimaryKey val date: LocalDate,
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
)

@Entity(tableName = "scale_screenshot_imports")
data class ScaleScreenshotImport(
    @PrimaryKey val id: String,
    val createdAtEpochMillis: Long,
    val localPath: String,
    val parsedWeightKg: Double? = null,
    val parsedBodyFatPercent: Double? = null,
    val parsedBmi: Double? = null,
    val parsedMuscleMassKg: Double? = null,
    val parsedSkeletalMuscleKg: Double? = null,
    val parsedWaterPercent: Double? = null,
    val parsedProteinPercent: Double? = null,
    val parsedVisceralFat: Int? = null,
    val parsedBasalMetabolismKcal: Int? = null,
    val parsedBiologicalAge: Int? = null,
    val parsedPulse: Int? = null,
    val isParsed: Boolean = false,
)

@Entity(tableName = "habit_entries")
data class HabitEntry(
    @PrimaryKey val date: LocalDate,
    val waterDone: Boolean,
    val stepsDone: Boolean,
    val proteinDone: Boolean,
    val workoutDone: Boolean,
    val postureDone: Boolean,
    val sleepDone: Boolean,
    val checkInDone: Boolean = false,
)

@Entity(tableName = "workout_plan_days", primaryKeys = ["week", "day"])
data class WorkoutPlanDay(
    val week: Int,
    val day: Int,
    val title: String,
    val exercises: List<String>,
    val focus: String,
)

@Entity(tableName = "imported_schedule_days")
data class ImportedScheduleDay(
    @PrimaryKey val date: LocalDate,
    val weekStartDate: LocalDate?,
    val focus: String?,
    val deliveryAddress: String?,
    val foodProviderName: String?,
    val foodProviderCity: String?,
    val foodProviderCityUrl: String?,
    val foodProviderNote: String?,
    val nutrition: List<String>,
    val recovery: List<String>,
    val checkpoints: List<String>,
)

@Entity(tableName = "imported_schedule_events")
data class ImportedScheduleEvent(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val time: String,
    val title: String,
    val description: String?,
    val kind: String,
    val isFixed: Boolean,
    val notificationText: String?,
)

@Entity(tableName = "schedule_event_completions")
data class ScheduleEventCompletion(
    @PrimaryKey val eventId: String,
    val date: LocalDate,
)

@Entity(tableName = "schedule_event_starts")
data class ScheduleEventStart(
    @PrimaryKey val eventId: String,
    val date: LocalDate,
)

@Entity(tableName = "imported_meal_recommendations")
data class ImportedMealRecommendation(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val time: String,
    val type: String,
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
    val source: String?,
    val fallback: String?,
)

@Entity(tableName = "imported_goals")
data class ImportedGoal(
    @PrimaryKey val id: String,
    val title: String,
    val visualReference: String?,
    val targetWeightKg: Double?,
    val targetBodyFatPercentRange: String?,
    val trainingPrinciples: List<String>,
    val focusMuscles: List<String>,
    val nutritionPrinciples: List<String>,
)

@Entity(tableName = "imported_workout_exercises")
data class ImportedWorkoutExercise(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val eventId: String?,
    val workoutId: String?,
    val workoutTitle: String?,
    val workoutDescription: String?,
    val workoutEstimatedDurationMin: Int?,
    val workoutIntensity: String?,
    val exerciseId: String,
    val orderIndex: Int,
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

data class Recommendation(
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
)

enum class RecommendationPriority { High, Medium, Low }
