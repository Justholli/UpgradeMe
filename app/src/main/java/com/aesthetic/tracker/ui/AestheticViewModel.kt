package com.aesthetic.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.MeasurementEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AestheticViewModel(private val repository: AestheticRepository) : ViewModel() {
    private val _state = MutableStateFlow(AestheticState())
    val state: StateFlow<AestheticState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureWorkoutPlan() }
        viewModelScope.launch {
            combine(repository.measurements, repository.habits, repository.workoutPlan) { measurements, habits, plan ->
                AestheticMutation.DataLoaded(measurements, habits, plan, LocalDate.now())
            }.collect { mutation -> commit(mutation) }
        }
    }

    fun dispatch(action: AestheticAction) {
        when (action) {
            is AestheticAction.SelectScreen -> commit(AestheticMutation.ScreenSelected(action.screen))
            is AestheticAction.ToggleHabit -> toggleHabit(action.habit)
            is AestheticAction.SaveMeasurement -> saveMeasurement(action)
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
                ),
            )
        }
    }

    class Factory(private val repository: AestheticRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AestheticViewModel(repository) as T
    }
}
