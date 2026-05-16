package com.aesthetic.tracker.data

import kotlinx.coroutines.flow.Flow

class AestheticRepository(private val dao: AestheticDao) {
    val measurements: Flow<List<MeasurementEntry>> = dao.observeMeasurements()
    val habits: Flow<List<HabitEntry>> = dao.observeHabits()
    val workoutPlan: Flow<List<WorkoutPlanDay>> = dao.observeWorkoutPlan()
    val scaleImports: Flow<List<ScaleScreenshotImport>> = dao.observeScaleImports()
    val importedScheduleDays: Flow<List<ImportedScheduleDay>> = dao.observeImportedScheduleDays()
    val importedScheduleEvents: Flow<List<ImportedScheduleEvent>> = dao.observeImportedScheduleEvents()
    val scheduleEventCompletions: Flow<List<ScheduleEventCompletion>> = dao.observeScheduleEventCompletions()
    val scheduleEventStarts: Flow<List<ScheduleEventStart>> = dao.observeScheduleEventStarts()
    val importedMealRecommendations: Flow<List<ImportedMealRecommendation>> = dao.observeImportedMealRecommendations()
    val importedWorkoutExercises: Flow<List<ImportedWorkoutExercise>> = dao.observeImportedWorkoutExercises()
    val importedGoal: Flow<ImportedGoal?> = dao.observeImportedGoal()

    suspend fun saveMeasurement(entry: MeasurementEntry) = dao.upsertMeasurement(entry)

    suspend fun getImportedScheduleEvents(): List<ImportedScheduleEvent> = dao.getImportedScheduleEvents()

    suspend fun saveHabit(entry: HabitEntry) = dao.upsertHabit(entry)

    suspend fun startScheduleEvent(entry: ScheduleEventStart) = dao.upsertScheduleEventStart(entry)

    suspend fun completeScheduleEvent(entry: ScheduleEventCompletion) = dao.upsertScheduleEventCompletion(entry)

    suspend fun uncompleteScheduleEvent(eventId: String) = dao.deleteScheduleEventCompletion(eventId)

    suspend fun saveScaleImport(entry: ScaleScreenshotImport) = dao.upsertScaleImport(entry)

    suspend fun deleteScaleImport(entry: ScaleScreenshotImport) = dao.deleteScaleImport(entry)

    suspend fun deleteMeasurement(entry: MeasurementEntry) = dao.deleteMeasurement(entry.date)

    suspend fun replaceImportedSchedule(
        days: List<ImportedScheduleDay>,
        events: List<ImportedScheduleEvent>,
        meals: List<ImportedMealRecommendation>,
        exercises: List<ImportedWorkoutExercise>,
        goal: ImportedGoal?,
    ) {
        val dates = days.map { it.date }.distinct()
        if (dates.isNotEmpty()) {
            dao.deleteScheduleEventStartsForDates(dates)
            dao.deleteScheduleEventCompletionsForDates(dates)
            dao.deleteImportedWorkoutExercisesForDates(dates)
            dao.deleteImportedMealRecommendationsForDates(dates)
            dao.deleteImportedScheduleEventsForDates(dates)
            dao.deleteImportedScheduleDaysForDates(dates)
        }
        if (days.isNotEmpty()) dao.upsertImportedScheduleDays(days)
        if (events.isNotEmpty()) dao.upsertImportedScheduleEvents(events)
        if (meals.isNotEmpty()) dao.upsertImportedMealRecommendations(meals)
        if (exercises.isNotEmpty()) dao.upsertImportedWorkoutExercises(exercises)
        if (goal != null) dao.upsertImportedGoal(goal)
    }

    suspend fun clearImportedSchedule() {
        dao.deleteImportedGoals()
        dao.deleteScheduleEventStarts()
        dao.deleteScheduleEventCompletions()
        dao.deleteImportedWorkoutExercises()
        dao.deleteImportedMealRecommendations()
        dao.deleteImportedScheduleEvents()
        dao.deleteImportedScheduleDays()
    }

    suspend fun ensureWorkoutPlan() {
        if (dao.workoutPlanCount() == 0) {
            dao.upsertWorkoutPlan(DefaultWorkoutPlan.days)
        }
    }
}
