package com.aesthetic.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.domain.AiScaleParser
import com.aesthetic.tracker.domain.buildAiAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class AestheticViewModel(private val repository: AestheticRepository) : ViewModel() {
    private val _state = MutableStateFlow(AestheticState())
    val state: StateFlow<AestheticState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureWorkoutPlan() }
        viewModelScope.launch {
            combine(repository.measurements, repository.habits, repository.workoutPlan, repository.scaleImports) { measurements, habits, plan, imports ->
                AestheticMutation.DataLoaded(measurements, habits, plan, imports, LocalDate.now())
            }.collect { mutation -> commit(mutation) }
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
            is AestheticAction.GenerateAiAnalysis -> commit(AestheticMutation.AiAnalysisGenerated(buildAiAnalysis(state.value.measurements)))
            is AestheticAction.SelectFoodGoal -> commit(AestheticMutation.FoodGoalSelected(action.goal))
        }
    }

    private fun commit(mutation: AestheticMutation) {
        _state.update { previous -> reduce(previous, mutation) }
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

    class Factory(private val repository: AestheticRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AestheticViewModel(repository) as T
    }
}
