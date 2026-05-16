package com.aesthetic.tracker.data

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("SELECT * FROM scale_screenshot_imports ORDER BY createdAtEpochMillis DESC")
    fun observeScaleImports(): Flow<List<ScaleScreenshotImport>>

    @Query("SELECT * FROM imported_schedule_days ORDER BY date ASC")
    fun observeImportedScheduleDays(): Flow<List<ImportedScheduleDay>>

    @Query("SELECT * FROM imported_schedule_events ORDER BY date ASC, time ASC")
    fun observeImportedScheduleEvents(): Flow<List<ImportedScheduleEvent>>

    @Query("SELECT * FROM imported_schedule_events ORDER BY date ASC, time ASC")
    suspend fun getImportedScheduleEvents(): List<ImportedScheduleEvent>

    @Query("SELECT * FROM schedule_event_completions ORDER BY date ASC, eventId ASC")
    fun observeScheduleEventCompletions(): Flow<List<ScheduleEventCompletion>>

    @Query("SELECT * FROM schedule_event_starts ORDER BY date ASC, eventId ASC")
    fun observeScheduleEventStarts(): Flow<List<ScheduleEventStart>>

    @Query("SELECT * FROM imported_meal_recommendations ORDER BY date ASC, time ASC")
    fun observeImportedMealRecommendations(): Flow<List<ImportedMealRecommendation>>

    @Query("SELECT * FROM imported_workout_exercises ORDER BY date ASC, eventId ASC, orderIndex ASC")
    fun observeImportedWorkoutExercises(): Flow<List<ImportedWorkoutExercise>>

    @Query("SELECT * FROM imported_goals WHERE id = 'current' LIMIT 1")
    fun observeImportedGoal(): Flow<ImportedGoal?>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScaleImport(entry: ScaleScreenshotImport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedScheduleDays(days: List<ImportedScheduleDay>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedScheduleEvents(events: List<ImportedScheduleEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleEventCompletion(completion: ScheduleEventCompletion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleEventStart(start: ScheduleEventStart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedMealRecommendations(meals: List<ImportedMealRecommendation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedWorkoutExercises(exercises: List<ImportedWorkoutExercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedGoal(goal: ImportedGoal)

    @Delete
    suspend fun deleteScaleImport(entry: ScaleScreenshotImport)

    @Query("DELETE FROM measurement_entries WHERE date = :date")
    suspend fun deleteMeasurement(date: LocalDate)

    @Query("DELETE FROM imported_schedule_days")
    suspend fun deleteImportedScheduleDays()

    @Query("DELETE FROM imported_schedule_events")
    suspend fun deleteImportedScheduleEvents()

    @Query("DELETE FROM schedule_event_completions")
    suspend fun deleteScheduleEventCompletions()

    @Query("DELETE FROM schedule_event_starts")
    suspend fun deleteScheduleEventStarts()

    @Query("DELETE FROM schedule_event_completions WHERE eventId = :eventId")
    suspend fun deleteScheduleEventCompletion(eventId: String)

    @Query("DELETE FROM imported_meal_recommendations")
    suspend fun deleteImportedMealRecommendations()

    @Query("DELETE FROM imported_workout_exercises")
    suspend fun deleteImportedWorkoutExercises()

    @Query("DELETE FROM imported_goals")
    suspend fun deleteImportedGoals()

    @Query("DELETE FROM imported_schedule_days WHERE date IN (:dates)")
    suspend fun deleteImportedScheduleDaysForDates(dates: List<LocalDate>)

    @Query("DELETE FROM imported_schedule_events WHERE date IN (:dates)")
    suspend fun deleteImportedScheduleEventsForDates(dates: List<LocalDate>)

    @Query("DELETE FROM schedule_event_completions WHERE date IN (:dates)")
    suspend fun deleteScheduleEventCompletionsForDates(dates: List<LocalDate>)

    @Query("DELETE FROM schedule_event_starts WHERE date IN (:dates)")
    suspend fun deleteScheduleEventStartsForDates(dates: List<LocalDate>)

    @Query("DELETE FROM imported_meal_recommendations WHERE date IN (:dates)")
    suspend fun deleteImportedMealRecommendationsForDates(dates: List<LocalDate>)

    @Query("DELETE FROM imported_workout_exercises WHERE date IN (:dates)")
    suspend fun deleteImportedWorkoutExercisesForDates(dates: List<LocalDate>)

    @Query("SELECT COUNT(*) FROM workout_plan_days")
    suspend fun workoutPlanCount(): Int
}
