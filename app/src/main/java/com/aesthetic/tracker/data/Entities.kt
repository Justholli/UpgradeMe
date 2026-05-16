package com.aesthetic.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "measurement_entries")
data class MeasurementEntry(
    @PrimaryKey val date: LocalDate,
    val bodyScore: Int? = null,
    val weightKg: Double,
    val bodyFatPercent: Double,
    val fatMassKg: Double? = null,
    val skeletalMuscleKg: Double,
    val muscleMassKg: Double? = null,
    val muscleRatePercent: Double? = null,
    val pulse: Int,
    val visceralFat: Int,
    val waterPercent: Double,
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
)

@Entity(tableName = "workout_plan_days", primaryKeys = ["week", "day"])
data class WorkoutPlanDay(
    val week: Int,
    val day: Int,
    val title: String,
    val exercises: List<String>,
    val focus: String,
)

data class Recommendation(
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
)

enum class RecommendationPriority { High, Medium, Low }
