package com.aesthetic.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AestheticDao {
    @Query("SELECT * FROM measurement_entries ORDER BY date DESC")
    fun observeMeasurements(): Flow<List<MeasurementEntry>>

    @Query("SELECT * FROM habit_entries ORDER BY date DESC")
    fun observeHabits(): Flow<List<HabitEntry>>

    @Query("SELECT * FROM workout_plan_days ORDER BY week ASC, day ASC")
    fun observeWorkoutPlan(): Flow<List<WorkoutPlanDay>>

    @Query("SELECT * FROM measurement_entries ORDER BY date DESC LIMIT 1")
    fun observeLatestMeasurement(): Flow<MeasurementEntry?>

    @Query("SELECT * FROM habit_entries WHERE date = :date LIMIT 1")
    fun observeHabitForDate(date: LocalDate): Flow<HabitEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(entry: MeasurementEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(entry: HabitEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutPlan(days: List<WorkoutPlanDay>)

    @Query("SELECT COUNT(*) FROM workout_plan_days")
    suspend fun workoutPlanCount(): Int
}
