package com.aesthetic.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aesthetic.tracker.data.AiSettingsStore
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.data.TodayPlanFileParser
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.AiScaleParser
import com.aesthetic.tracker.domain.GeneratedTodayPlan
import com.aesthetic.tracker.notifications.TodayNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AestheticViewModel @Inject constructor(
    private val repository: AestheticRepository,
    private val aiSettingsStore: AiSettingsStore,
    private val todayPlanFileParser: TodayPlanFileParser,
    private val notificationScheduler: TodayNotificationScheduler,
) : ViewModel() {
    private val _state = MutableStateFlow(AestheticState())
    val state: StateFlow<AestheticState> = _state.asStateFlow()
    private var scheduledPlanKey: String? = null

    init {
        viewModelScope.launch {
            repository.ensureWorkoutPlan()
            val loadedCore = combine(repository.measurements, repository.habits, repository.workoutPlan, repository.scaleImports, todayTicker()) { measurements, habits, plan, imports, today ->
                LoadedCore(measurements, habits, plan, imports, today)
            }
            combine(loadedCore, aiSettingsStore.chatUrl) { core, chatUrl ->
                AestheticMutation.DataLoaded(core.measurements, core.habits, core.workoutPlan, core.scaleImports, chatUrl, core.today)
            }.collect { mutation ->
                commit(mutation)
                scheduleNotificationsForCurrentPlan()
            }
        }
    }

    fun dispatch(action: AestheticAction) {
        when (action) {
            is AestheticAction.SelectScreen -> commit(AestheticMutation.ScreenSelected(action.screen))
            is AestheticAction.ToggleHabit -> toggleHabit(action.habit)
            is AestheticAction.SaveMeasurement -> saveMeasurement(action)
            is AestheticAction.SaveScaleImport -> saveScaleImport(action)
            is AestheticAction.ParseScaleImport -> parseScaleImport(action)
            is AestheticAction.ConfirmParsedScaleImport -> confirmParsedScaleImport(action)
            is AestheticAction.DeleteScaleImport -> deleteScaleImport(action)
            is AestheticAction.DeleteMeasurement -> deleteMeasurement(action)
            is AestheticAction.ImportTodayPlan -> importTodayPlan(action.content)
            is AestheticAction.ToggleScheduleItem -> commit(AestheticMutation.ScheduleItemToggled(action.itemId))
            is AestheticAction.SaveChatUrl -> saveChatUrl(action.url)
            is AestheticAction.SelectFoodGoal -> commit(AestheticMutation.FoodGoalSelected(action.goal))
        }
    }

    fun scheduleNotificationsForCurrentPlan() {
        val plan = state.value.generatedTodayPlan ?: return
        val planKey = plan.notificationKey(state.value.today)
        if (scheduledPlanKey == planKey) return
        if (notificationScheduler.scheduleToday(plan.schedule, state.value.today)) {
            scheduledPlanKey = planKey
        }
    }

    private fun commit(mutation: AestheticMutation) {
        _state.update { previous -> reduce(previous, mutation) }
    }

    private fun importTodayPlan(content: String) {
        todayPlanFileParser.parse(content, state.value.todayPlanInput())
            .onSuccess { imported ->
                viewModelScope.launch {
                    imported.measurement?.let { repository.saveMeasurement(it) }
                    commit(AestheticMutation.TodayPlanGenerated(imported.plan))
                    scheduleNotificationsForCurrentPlan()
                }
            }
            .onFailure { error ->
                commit(AestheticMutation.TodayPlanGenerationFailed(error.message ?: "Не удалось импортировать файл расписания."))
            }
    }

    private fun saveChatUrl(url: String) {
        aiSettingsStore.saveChatUrl(url)
        commit(AestheticMutation.ChatUrlSaved(url.trim()))
    }

    private fun toggleHabit(kind: HabitKind) {
        viewModelScope.launch {
            val current = state.value.currentHabit
            val updated = when (kind) {
                HabitKind.Water -> current.copy(waterDone = !current.waterDone)
                HabitKind.Steps -> current.copy(stepsDone = !current.stepsDone)
                HabitKind.Protein -> current.copy(proteinDone = !current.proteinDone)
                HabitKind.Workout -> current.copy(workoutDone = !current.workoutDone)
                HabitKind.Posture -> current.copy(postureDone = !current.postureDone)
                HabitKind.Sleep -> current.copy(sleepDone = !current.sleepDone)
            }
            repository.saveHabit(updated)
        }
    }

    private fun saveMeasurement(action: AestheticAction.SaveMeasurement) {
        viewModelScope.launch {
            repository.saveMeasurement(
                MeasurementEntry(
                    date = LocalDate.now(),
                    bodyScore = action.bodyScore,
                    weightKg = action.weightKg,
                    bodyFatPercent = action.bodyFatPercent,
                    fatMassKg = action.fatMassKg,
                    skeletalMuscleKg = action.skeletalMuscleKg,
                    muscleMassKg = action.muscleMassKg,
                    muscleRatePercent = action.muscleRatePercent,
                    pulse = action.pulse,
                    visceralFat = action.visceralFat,
                    waterPercent = action.waterPercent,
                    bodyWaterKg = action.bodyWaterKg,
                    bmi = action.bmi,
                    mineralMassKg = action.mineralMassKg,
                    proteinMassKg = action.proteinMassKg,
                    proteinPercent = action.proteinPercent,
                    subcutaneousFatPercent = action.subcutaneousFatPercent,
                    leanBodyMassKg = action.leanBodyMassKg,
                    basalMetabolismKcal = action.basalMetabolismKcal,
                    biologicalAge = action.biologicalAge,
                    bodyType = action.bodyType?.trim()?.takeIf { it.isNotBlank() },
                    standardWeightKg = action.standardWeightKg,
                    weightControlKg = action.weightControlKg,
                    fatControlKg = action.fatControlKg,
                    muscleControlKg = action.muscleControlKg,
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
            repository.saveScaleImport(AiScaleParser.parse(action.scaleImport, state.value.measurements.firstOrNull()))
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
        }
    }

    private fun deleteScaleImport(action: AestheticAction.DeleteScaleImport) {
        viewModelScope.launch {
            File(action.scaleImport.localPath).delete()
            repository.deleteScaleImport(action.scaleImport)
        }
    }

    private fun deleteMeasurement(action: AestheticAction.DeleteMeasurement) {
        viewModelScope.launch {
            action.measurement.scalePhotoPath?.let { File(it).delete() }
            repository.deleteMeasurement(action.measurement)
        }
    }
}

private fun GeneratedTodayPlan.notificationKey(today: LocalDate): String =
    listOf(
        signature.toString(),
        today.toString(),
        schedule.joinToString("|") { item ->
            listOf(
                item.id,
                item.time,
                item.title,
                item.notificationText,
            ).joinToString(":")
        },
    ).joinToString("::")

private data class LoadedCore(
    val measurements: List<MeasurementEntry>,
    val habits: List<HabitEntry>,
    val workoutPlan: List<WorkoutPlanDay>,
    val scaleImports: List<ScaleScreenshotImport>,
    val today: LocalDate,
)

private fun todayTicker() = flow {
    while (true) {
        emit(LocalDate.now())
        delay(60_000)
    }
}.distinctUntilChanged()
