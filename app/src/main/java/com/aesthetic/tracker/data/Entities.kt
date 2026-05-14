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
