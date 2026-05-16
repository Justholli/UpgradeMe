package com.aesthetic.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.ImportedGoal
import com.aesthetic.tracker.data.ImportedMealRecommendation
import com.aesthetic.tracker.data.ImportedScheduleDay
import com.aesthetic.tracker.data.ImportedScheduleEvent
import com.aesthetic.tracker.data.ImportedWorkoutExercise
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.data.ScheduleEventCompletion
import com.aesthetic.tracker.data.ScheduleEventStart
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.AiScaleParser
import com.aesthetic.tracker.domain.buildGeneratedTodayPlan
import com.aesthetic.tracker.notification.EventReminderPlanner
import com.aesthetic.tracker.notification.NoOpEventReminderPlanner
import com.aesthetic.tracker.ui.dashboard.DashboardPlanImportResult
import com.aesthetic.tracker.ui.dashboard.ImportParseResult
import com.aesthetic.tracker.ui.dashboard.JsonDashboardPlanImporter
import com.aesthetic.tracker.ui.dashboard.JsonMeasurementImporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class AestheticViewModel(
    private val repository: AestheticRepository,
    private val eventReminderPlanner: EventReminderPlanner = NoOpEventReminderPlanner,
) : ViewModel() {
    private val _state = MutableStateFlow(AestheticState())
    val state: StateFlow<AestheticState> = _state.asStateFlow()
    private val habitMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.ensureWorkoutPlan()
            eventReminderPlanner.scheduleNextReminder()
            val coreData = combine(repository.measurements, repository.habits, repository.workoutPlan, repository.scaleImports) { measurements, habits, plan, imports ->
                CoreData(measurements, habits, plan, imports)
            }
            val importedScheduleCore = combine(
                repository.importedScheduleDays,
                repository.importedScheduleEvents,
                repository.scheduleEventCompletions,
                repository.scheduleEventStarts,
            ) { days, events, completions, starts ->
                ImportedScheduleCore(days, events, completions, starts)
            }
            val importedContent = combine(
                importedScheduleCore,
                repository.importedMealRecommendations,
                repository.importedWorkoutExercises,
            ) { schedule, meals, exercises ->
                ImportedContent(schedule.days, schedule.events, schedule.completions, schedule.starts, meals, exercises)
            }
            val importedData = combine(
                importedContent,
                repository.importedGoal,
                todayTicker(),
            ) { content, goal, today ->
                ImportedData(content.days, content.events, content.completions, content.starts, content.meals, content.exercises, goal, today)
            }
            combine(coreData, importedData) { core, imported ->
                AestheticMutation.DataLoaded(
                    measurements = core.measurements,
                    habits = core.habits,
                    workoutPlan = core.workoutPlan,
                    scaleImports = core.scaleImports,
                    importedScheduleDays = imported.days,
                    importedScheduleEvents = imported.events,
                    scheduleEventCompletions = imported.completions,
                    scheduleEventStarts = imported.starts,
                    importedMealRecommendations = imported.meals,
                    importedWorkoutExercises = imported.exercises,
                    importedGoal = imported.goal,
                    today = imported.today,
                )
            }.collect { mutation -> commit(mutation) }
        }
    }

    fun dispatch(action: AestheticAction) {
        when (action) {
            is AestheticAction.SelectScreen -> commit(AestheticMutation.ScreenSelected(action.screen))
            is AestheticAction.ToggleHabit -> toggleHabit(action.habit)
            is AestheticAction.ToggleScheduleEventCompletion -> toggleScheduleEventCompletion(action.eventId)
            is AestheticAction.SaveMeasurement -> saveMeasurement(action)
            is AestheticAction.SaveScaleImport -> saveScaleImport(action)
            is AestheticAction.ParseScaleImport -> parseScaleImport(action)
            is AestheticAction.ConfirmParsedScaleImport -> confirmParsedScaleImport(action)
            is AestheticAction.DeleteScaleImport -> deleteScaleImport(action)
            is AestheticAction.DeleteMeasurement -> deleteMeasurement(action)
            is AestheticAction.GenerateTodayPlan -> commit(AestheticMutation.TodayPlanGenerated(buildGeneratedTodayPlan(state.value.todayPlanInput())))
            is AestheticAction.SelectFoodGoal -> commit(AestheticMutation.FoodGoalSelected(action.goal))
            is AestheticAction.SubmitJsonImport -> importJson(action.json)
            is AestheticAction.StartEventProgressNotification -> startScheduleEvent(action.eventId)
            AestheticAction.DismissImportStatus -> commit(AestheticMutation.ImportDismissed)
            AestheticAction.ClearImportedSchedule -> clearImportedSchedule()
        }
    }

    private fun commit(mutation: AestheticMutation) {
        _state.update { previous -> reduce(previous, mutation) }
    }

    private fun toggleHabit(kind: HabitKind) {
        viewModelScope.launch {
            habitMutex.withLock {
                val current = state.value.currentHabit
                val updated = when (kind) {
                    HabitKind.Water -> current.copy(waterDone = !current.waterDone)
                    HabitKind.Steps -> current.copy(stepsDone = !current.stepsDone)
                    HabitKind.Protein -> current.copy(proteinDone = !current.proteinDone)
                    HabitKind.Workout -> current.copy(workoutDone = !current.workoutDone)
                    HabitKind.Posture -> current.copy(postureDone = !current.postureDone)
                    HabitKind.Sleep -> current.copy(sleepDone = !current.sleepDone)
                    HabitKind.CheckIn -> current.copy(checkInDone = !current.checkInDone)
                }
                repository.saveHabit(updated)
            }
        }
    }

    private fun startScheduleEvent(eventId: String) {
        viewModelScope.launch {
            val event = state.value.dashboard.today.events.firstOrNull { it.id == eventId } ?: return@launch
            if (event.status != com.aesthetic.tracker.ui.dashboard.ScheduleEventStatus.Active) return@launch
            repository.startScheduleEvent(ScheduleEventStart(eventId = event.id, date = state.value.today))
        }
    }

    private fun toggleScheduleEventCompletion(eventId: String) {
        viewModelScope.launch {
            val currentState = state.value
            val event = currentState.dashboard.today.events.firstOrNull { it.id == eventId } ?: return@launch
            if (!event.canToggleCompletion) return@launch
            val isImportedEvent = currentState.importedScheduleEvents.any { it.id == event.id }
            if (isImportedEvent) {
                if (event.isCompleted) {
                    repository.uncompleteScheduleEvent(event.id)
                } else {
                    repository.completeScheduleEvent(ScheduleEventCompletion(eventId = event.id, date = currentState.today))
                }
                return@launch
            }
            event.habitKind?.let { toggleHabit(it) }
        }
    }

    private fun clearImportedSchedule() {
        viewModelScope.launch {
            repository.clearImportedSchedule()
            eventReminderPlanner.cancelReminder()
            commit(AestheticMutation.ImportSucceeded("Импортированное расписание очищено."))
        }
    }

    private fun saveMeasurement(action: AestheticAction.SaveMeasurement) {
        viewModelScope.launch {
            repository.saveMeasurement(
                MeasurementEntry(
                    date = LocalDate.now(),
                    weightKg = action.weightKg,
                    bodyFatPercent = action.bodyFatPercent,
                    skeletalMuscleKg = action.skeletalMuscleKg,
                    pulse = action.pulse,
                    visceralFat = action.visceralFat,
                    waterPercent = action.waterPercent,
                    bmi = action.bmi,
                    muscleMassKg = action.muscleMassKg,
                    proteinPercent = action.proteinPercent,
                    basalMetabolismKcal = action.basalMetabolismKcal,
                    biologicalAge = action.biologicalAge,
                    scalePhotoPath = action.scalePhotoPath,
                ),
            )
        }
    }


    private fun saveScaleImport(action: AestheticAction.SaveScaleImport) {
        viewModelScope.launch { repository.saveScaleImport(action.scaleImport) }
    }

    private fun parseScaleImport(action: AestheticAction.ParseScaleImport) {
        viewModelScope.launch {
            commit(AestheticMutation.ImportStarted)
            runCatching {
                repository.saveScaleImport(AiScaleParser.parse(action.scaleImport, state.value.measurements.firstOrNull()))
            }.onSuccess {
                commit(AestheticMutation.ImportSucceeded("Скриншот распознан. Проверьте значения перед сохранением."))
            }.onFailure {
                commit(AestheticMutation.ImportFailed("Не удалось распознать скриншот. Попробуйте другой файл или внесите значения вручную."))
            }
        }
    }

    private fun confirmParsedScaleImport(action: AestheticAction.ConfirmParsedScaleImport) {
        viewModelScope.launch {
            val import = action.scaleImport
            repository.saveMeasurement(
                MeasurementEntry(
                    date = LocalDate.now(),
                    weightKg = import.parsedWeightKg ?: state.value.measurements.firstOrNull()?.weightKg ?: 0.0,
                    bodyFatPercent = import.parsedBodyFatPercent ?: state.value.measurements.firstOrNull()?.bodyFatPercent ?: 0.0,
                    skeletalMuscleKg = import.parsedSkeletalMuscleKg ?: state.value.measurements.firstOrNull()?.skeletalMuscleKg ?: 0.0,
                    pulse = import.parsedPulse ?: state.value.measurements.firstOrNull()?.pulse ?: 0,
                    visceralFat = import.parsedVisceralFat ?: state.value.measurements.firstOrNull()?.visceralFat ?: 0,
                    waterPercent = import.parsedWaterPercent ?: state.value.measurements.firstOrNull()?.waterPercent ?: 0.0,
                    bmi = import.parsedBmi,
                    muscleMassKg = import.parsedMuscleMassKg,
                    proteinPercent = import.parsedProteinPercent,
                    basalMetabolismKcal = import.parsedBasalMetabolismKcal,
                    biologicalAge = import.parsedBiologicalAge,
                    scalePhotoPath = import.localPath,
                ),
            )
            commit(AestheticMutation.ImportSucceeded("Замер сохранен из импортированных данных."))
        }
    }

    private fun deleteScaleImport(action: AestheticAction.DeleteScaleImport) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { File(action.scaleImport.localPath).delete() }
            repository.deleteScaleImport(action.scaleImport)
        }
    }

    private fun deleteMeasurement(action: AestheticAction.DeleteMeasurement) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { action.measurement.scalePhotoPath?.let { File(it).delete() } }
            repository.deleteMeasurement(action.measurement)
        }
    }

    private fun importJson(json: String) {
        viewModelScope.launch {
            commit(AestheticMutation.ImportStarted)
            runCatching {
                when (val planResult = JsonDashboardPlanImporter.parse(json, LocalDate.now(), state.value.measurements.firstOrNull())) {
                    is DashboardPlanImportResult.Success -> {
                        planResult.measurement?.let { repository.saveMeasurement(it) }
                        repository.replaceImportedSchedule(planResult.days, planResult.events, planResult.meals, planResult.exercises, planResult.goal)
                        eventReminderPlanner.scheduleNextReminder()
                        commit(AestheticMutation.ImportSucceeded("Расписание и рекомендации еды импортированы из JSON."))
                        return@launch
                    }
                    is DashboardPlanImportResult.Error -> {
                        commit(AestheticMutation.ImportFailed(planResult.message))
                        return@launch
                    }
                    DashboardPlanImportResult.NotDashboardPlan -> Unit
                }
                when (val result = JsonMeasurementImporter.parse(json, LocalDate.now(), state.value.measurements.firstOrNull())) {
                    is ImportParseResult.Success -> {
                        repository.saveMeasurement(result.measurement)
                        commit(AestheticMutation.ImportSucceeded("Замер импортирован из JSON."))
                    }
                    is ImportParseResult.Error -> commit(AestheticMutation.ImportFailed(result.message))
                }
            }.onFailure {
                commit(AestheticMutation.ImportFailed("Не удалось сохранить импортированные данные. Попробуйте еще раз."))
            }
        }
    }

    class Factory(
        private val repository: AestheticRepository,
        private val eventReminderPlanner: EventReminderPlanner = NoOpEventReminderPlanner,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AestheticViewModel(repository, eventReminderPlanner) as T
    }
}

private data class CoreData(
    val measurements: List<MeasurementEntry>,
    val habits: List<HabitEntry>,
    val workoutPlan: List<WorkoutPlanDay>,
    val scaleImports: List<ScaleScreenshotImport>,
)

private data class ImportedScheduleCore(
    val days: List<ImportedScheduleDay>,
    val events: List<ImportedScheduleEvent>,
    val completions: List<ScheduleEventCompletion>,
    val starts: List<ScheduleEventStart>,
)

private data class ImportedContent(
    val days: List<ImportedScheduleDay>,
    val events: List<ImportedScheduleEvent>,
    val completions: List<ScheduleEventCompletion>,
    val starts: List<ScheduleEventStart>,
    val meals: List<ImportedMealRecommendation>,
    val exercises: List<ImportedWorkoutExercise>,
)

private data class ImportedData(
    val days: List<ImportedScheduleDay>,
    val events: List<ImportedScheduleEvent>,
    val completions: List<ScheduleEventCompletion>,
    val starts: List<ScheduleEventStart>,
    val meals: List<ImportedMealRecommendation>,
    val exercises: List<ImportedWorkoutExercise>,
    val goal: ImportedGoal?,
    val today: LocalDate,
)

private fun todayTicker() = flow {
    while (true) {
        emit(LocalDate.now())
        delay(60_000)
    }
}.distinctUntilChanged()
