package com.aesthetic.tracker.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AestheticRepository(private val dao: AestheticDao) {
    val measurements: Flow<List<MeasurementEntry>> = dao.observeMeasurements()
    val habits: Flow<List<HabitEntry>> = dao.observeHabits()
    val workoutPlan: Flow<List<WorkoutPlanDay>> = dao.observeWorkoutPlan()
    val scaleImports: Flow<List<ScaleScreenshotImport>> = dao.observeScaleImports()
    val latestMeasurement: Flow<MeasurementEntry?> = dao.observeLatestMeasurement()

    fun habitForDate(date: LocalDate): Flow<HabitEntry?> = dao.observeHabitForDate(date)

    suspend fun saveMeasurement(entry: MeasurementEntry) = dao.upsertMeasurement(entry)

    suspend fun saveHabit(entry: HabitEntry) = dao.upsertHabit(entry)

    suspend fun saveScaleImport(entry: ScaleScreenshotImport) = dao.upsertScaleImport(entry)

    suspend fun deleteScaleImport(entry: ScaleScreenshotImport) = dao.deleteScaleImport(entry)

    suspend fun deleteMeasurement(entry: MeasurementEntry) = dao.deleteMeasurement(entry.date)

    suspend fun ensureWorkoutPlan() {
        if (dao.workoutPlanCount() == 0) {
            dao.upsertWorkoutPlan(DefaultWorkoutPlan.days)
        }
    }
}
