package com.aesthetic.tracker.ui

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.ImportedGoal
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.ui.dashboard.ImportUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AestheticReducerTest {
    @Test
    fun `data loaded builds grouped dashboard state`() {
        val today = LocalDate.of(2026, 5, 16)
        val habit = HabitEntry(today, waterDone = true, stepsDone = true, proteinDone = false, workoutDone = false, postureDone = false, sleepDone = false)

        val state = reduce(
            AestheticState(today = today),
            AestheticMutation.DataLoaded(
                measurements = emptyList(),
                habits = listOf(habit),
                workoutPlan = listOf(WorkoutPlanDay(1, 1, "Workout", listOf("Squat"), "Strength")),
                scaleImports = emptyList(),
                importedScheduleDays = emptyList(),
                importedScheduleEvents = emptyList(),
                scheduleEventCompletions = emptyList(),
                scheduleEventStarts = emptyList(),
                importedMealRecommendations = emptyList(),
                importedWorkoutExercises = emptyList(),
                importedGoal = null,
                today = today,
            ),
        )

        assertFalse(state.isLoading)
        assertEquals(7, state.dashboard.week.size)
        assertEquals(today, state.dashboard.today.date)
        assertTrue(state.dashboard.today.completedCount >= 2)
    }

    @Test
    fun `data loaded keeps imported goal in state`() {
        val today = LocalDate.of(2026, 5, 16)
        val goal = ImportedGoal(
            id = "current",
            title = "V-образная сухая форма",
            visualReference = "Широкие плечи и ровная осанка",
            targetWeightKg = 69.3,
            targetBodyFatPercentRange = "14-16",
            trainingPrinciples = listOf("Домашние тренировки"),
            focusMuscles = listOf("плечи"),
            nutritionPrinciples = listOf("Белок 120-140 г"),
        )

        val state = reduce(
            AestheticState(today = today),
            AestheticMutation.DataLoaded(
                measurements = emptyList(),
                habits = emptyList(),
                workoutPlan = emptyList(),
                scaleImports = emptyList(),
                importedScheduleDays = emptyList(),
                importedScheduleEvents = emptyList(),
                scheduleEventCompletions = emptyList(),
                scheduleEventStarts = emptyList(),
                importedMealRecommendations = emptyList(),
                importedWorkoutExercises = emptyList(),
                importedGoal = goal,
                today = today,
            ),
        )

        assertEquals(goal, state.importedGoal)
    }

    @Test
    fun `import mutations expose explicit ui state`() {
        val loading = reduce(AestheticState(), AestheticMutation.ImportStarted)
        val success = reduce(loading, AestheticMutation.ImportSucceeded("Done"))
        val dismissed = reduce(success, AestheticMutation.ImportDismissed)

        assertTrue(loading.importUiState is ImportUiState.Loading)
        assertEquals(ImportUiState.Success("Done"), success.importUiState)
        assertEquals(ImportUiState.Idle, dismissed.importUiState)
    }
}
