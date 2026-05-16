package com.aesthetic.tracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.ImportedGoal
import com.aesthetic.tracker.data.ImportedMealRecommendation
import com.aesthetic.tracker.data.ImportedScheduleDay
import com.aesthetic.tracker.data.ImportedScheduleEvent
import com.aesthetic.tracker.data.ImportedWorkoutExercise
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScheduleEventStart
import com.aesthetic.tracker.data.WorkoutPlanDay
import com.aesthetic.tracker.domain.PlanPosition
import com.aesthetic.tracker.ui.dashboard.DashboardUiMapper
import com.aesthetic.tracker.ui.dashboard.CalendarScreen
import com.aesthetic.tracker.ui.dashboard.ImportSheetContent
import com.aesthetic.tracker.ui.dashboard.ImportUiState
import com.aesthetic.tracker.ui.dashboard.ProgressScreen
import com.aesthetic.tracker.ui.dashboard.TodayScreen
import com.aesthetic.tracker.ui.theme.AestheticTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AestheticAppRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomNavigationSwitchesDashboardScreens() {
        var appState by mutableStateOf(fakeState())
        composeRule.setContent {
            AestheticTrackerTheme {
                AestheticApp(
                    state = appState,
                    onAction = { action ->
                        if (action is AestheticAction.SelectScreen) {
                            appState = appState.copy(selectedScreen = action.screen)
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("screen_Today").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_Calendar").performClick()
        composeRule.onNodeWithTag("screen_Calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_Progress").performClick()
        composeRule.onNodeWithTag("screen_Progress").assertIsDisplayed()
    }

    @Test
    fun importBottomSheetSubmitsJsonAction() {
        val actions = mutableListOf<AestheticAction>()
        composeRule.setContent {
            AestheticTrackerTheme {
                ImportSheetContent(
                    importUiState = ImportUiState.Idle,
                    json = "",
                    onJsonChange = {},
                    onSubmitJson = { actions += AestheticAction.SubmitJsonImport("""{"weightKg":70.2}""") },
                    onPickJsonFile = { actions += AestheticAction.SubmitJsonImport("""{"weightKg":70.3}""") },
                    onPickImage = {},
                    onOpenChatGpt = {},
                )
            }
        }

        composeRule.onNodeWithText("JSON замера или расписания").assertIsDisplayed()
        composeRule.onNodeWithTag("importJsonButton").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(AestheticAction.SubmitJsonImport("""{"weightKg":70.2}""")), actions)
    }

    @Test
    fun importSheetFileButtonSubmitsImmediately() {
        var pickedFile = false
        var dismissed = false
        composeRule.setContent {
            AestheticTrackerTheme {
                ImportSheetContent(
                    importUiState = ImportUiState.Idle,
                    json = "",
                    onJsonChange = {},
                    onSubmitJson = {},
                    onPickJsonFile = { pickedFile = true },
                    onPickImage = {},
                    onOpenChatGpt = {},
                    onImportStarted = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("importJsonFileButton").performClick()

        assertTrue(pickedFile)
        assertEquals(false, dismissed)
    }

    @Test
    fun progressScreenHasOneCombinedImportEntryPoint() {
        composeRule.setContent {
            AestheticTrackerTheme {
                AestheticApp(state = fakeState().copy(selectedScreen = TrackerScreen.Progress), onAction = {})
            }
        }

        composeRule.onNodeWithTag("openImportSheetButton").assertIsDisplayed()
    }

    @Test
    fun progressScreenShowsImportedGoalAndCurrentState() {
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
        val measurement = MeasurementEntry(
            date = today,
            weightKg = 70.4,
            bodyFatPercent = 18.0,
            skeletalMuscleKg = 29.0,
            pulse = 102,
            visceralFat = 7,
            waterPercent = 56.8,
        )
        val state = fakeState().copy(
            selectedScreen = TrackerScreen.Progress,
            importedGoal = goal,
            measurements = listOf(measurement),
        )

        composeRule.setContent {
            AestheticTrackerTheme {
                ProgressScreen(state = state.copy(dashboard = DashboardUiMapper.map(state)), onAction = {})
            }
        }

        composeRule.onNodeWithTag("goalProgressCard").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Конечная цель").assertIsDisplayed()
        composeRule.onNodeWithText("V-образная сухая форма").assertIsDisplayed()
        composeRule.onNodeWithText("Текущее состояние").assertIsDisplayed()
        composeRule.onNodeWithText("Вес: 70,4 кг / цель 69,3 кг").assertIsDisplayed()
    }

    @Test
    fun progressImportSheetClosesAfterManualJsonImport() {
        val actions = mutableListOf<AestheticAction>()
        var dismissed = false
        composeRule.setContent {
            AestheticTrackerTheme {
                ImportSheetContent(
                    importUiState = ImportUiState.Idle,
                    json = """{"weightKg":70.2}""",
                    onJsonChange = {},
                    onSubmitJson = { actions += AestheticAction.SubmitJsonImport("""{"weightKg":70.2}""") },
                    onPickJsonFile = {},
                    onPickImage = {},
                    onOpenChatGpt = {},
                    onImportStarted = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("importBottomSheet").assertIsDisplayed()
        composeRule.onNodeWithTag("importJsonButton").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(AestheticAction.SubmitJsonImport("""{"weightKg":70.2}""")), actions)
        assertTrue(dismissed)
    }

    @Test
    fun todayTopNextEventOpensDetails() {
        composeRule.setContent {
            AestheticTrackerTheme {
                AestheticApp(state = fakeState(), onAction = {})
            }
        }

        composeRule.onNodeWithTag("nextEventCard").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("nextEventCountdownRing", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("nextEventCard").performClick()

        composeRule.onNodeWithTag("eventDetailsSheet").assertIsDisplayed()
    }

    @Test
    fun todayShowsCurrentAndNextEventCards() {
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = fakeStateWithActiveEvent(), onAction = {})
            }
        }

        val state = fakeStateWithActiveEvent()
        assertTrue(DashboardUiMapper.currentEvent(state.dashboard.today) != null)
        assertTrue(DashboardUiMapper.nextUpcomingEvent(state.dashboard.today) != null)
        composeRule.onNodeWithTag("currentEventCard").assertIsDisplayed()
    }

    @Test
    fun currentEventCardCanCollapseAndStartProgressNotification() {
        val actions = mutableListOf<AestheticAction>()
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = fakeStateWithActiveEvent(), onAction = { actions += it })
            }
        }

        composeRule.onNodeWithTag("currentEventCard_progressButton").performClick()
        assertTrue(actions.single() is AestheticAction.StartEventProgressNotification)

        composeRule.onNodeWithTag("currentEventCard_collapseButton").performClick()
        composeRule.onAllNodesWithTag("currentEventCard_progressButton").assertCountEquals(0)
        composeRule.onNodeWithTag("currentEventCard").assertIsDisplayed()
    }

    @Test
    fun startedCurrentEventShowsWorkStateAndCompletionAction() {
        val actions = mutableListOf<AestheticAction>()
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = fakeStateWithStartedActiveEvent(), onAction = { actions += it })
            }
        }

        composeRule.onNodeWithText("В работе").assertIsDisplayed()
        composeRule.onNodeWithText("Выполнено").assertIsDisplayed()
        composeRule.onNodeWithTag("currentEventCard_progressButton").performClick()

        assertEquals(listOf(AestheticAction.ToggleScheduleEventCompletion("current-meal")), actions)
    }

    @Test
    fun todayShowsRecommendedFoodWhenNextEventIsMeal() {
        val state = fakeStateWithImportedMeal()
        assertEquals("Боул с курицей", DashboardUiMapper.nextEvent(state.dashboard.today)?.mealRecommendation?.title)

        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = state, onAction = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nextMealRecommendation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Боул с курицей").assertIsDisplayed()
        composeRule.onNodeWithTag("openFoodUrlButton").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun todayWorkoutDetailsShowsExerciseCards() {
        val state = fakeStateWithImportedWorkout()
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = state, onAction = {})
            }
        }

        composeRule.onNodeWithTag("nextEventCard").performScrollTo().performClick()

        composeRule.onNodeWithTag("eventDetailsSheet").assertIsDisplayed()
        composeRule.onNodeWithText("Упражнения").assertIsDisplayed()
        composeRule.onNodeWithTag("exerciseCard_wall-slides").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("4 x 10-12 · отдых 30 сек · RPE 5").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exerciseCardOpensNestedExerciseBottomSheet() {
        val state = fakeStateWithImportedWorkout()
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = state, onAction = {})
            }
        }

        composeRule.onNodeWithTag("nextEventCard").performScrollTo().performClick()
        composeRule.onNodeWithTag("exerciseCard_wall-slides").performScrollTo()
        composeRule.onNodeWithTag("exerciseCard_wall-slides", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("exerciseDetailsSheet")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("exerciseDetailsSheet").assertIsDisplayed()
    }

    @Test
    fun todayTimelineWorkoutDetailsButtonOpensDetails() {
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = fakeStateWithImportedWorkout(), onAction = {})
            }
        }

        composeRule.onNodeWithText("Таймлайн").performScrollTo()
        composeRule.onNodeWithTag("screen_Today").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("timelineEvent_workout").performScrollTo().performClick()

        composeRule.onNodeWithTag("eventDetailsSheet").assertIsDisplayed()
        composeRule.onNodeWithText("Упражнения").assertIsDisplayed()
        composeRule.onNodeWithTag("exerciseCard_wall-slides").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun upcomingWorkoutDetailsDoesNotShowCompletionButton() {
        val actions = mutableListOf<AestheticAction>()
        composeRule.setContent {
            AestheticTrackerTheme {
                TodayScreen(state = fakeStateWithImportedWorkout(), onAction = { actions += it })
            }
        }

        composeRule.onNodeWithTag("nextEventCard").performScrollTo().performClick()
        composeRule.onNodeWithTag("eventDetailsSheet").assertIsDisplayed()
        composeRule.onAllNodesWithTag("toggleEventDoneButton").assertCountEquals(0)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun progressImportCardClearImportedScheduleDispatchesAction() {
        val actions = mutableListOf<AestheticAction>()
        composeRule.setContent {
            AestheticTrackerTheme {
                AestheticApp(
                    state = fakeState().copy(selectedScreen = TrackerScreen.Progress),
                    onAction = { actions += it },
                )
            }
        }

        composeRule.onNodeWithTag("clearImportedScheduleButton").performClick()

        assertEquals(listOf(AestheticAction.ClearImportedSchedule), actions)
    }

    @Test
    fun calendarCanSwitchToMonthMode() {
        composeRule.setContent {
            AestheticTrackerTheme {
                CalendarScreen(state = fakeState())
            }
        }

        composeRule.onNodeWithTag("calendarMode_Month").performClick()

        composeRule.onNodeWithTag("calendarDay_2026-05-16").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("calendarDay_2026-04-30").assertCountEquals(0)
        composeRule.onAllNodesWithTag("calendarDay_2026-06-01").assertCountEquals(0)
    }

    private fun fakeState(): AestheticState {
        val today = LocalDate.of(2026, 5, 16)
        val habit = HabitEntry(today, waterDone = true, stepsDone = false, proteinDone = false, workoutDone = false, postureDone = false, sleepDone = false)
        val state = AestheticState(
            today = today,
            habits = listOf(habit),
            currentHabit = habit,
            workoutPlan = listOf(WorkoutPlanDay(1, 1, "Upper body", listOf("Push-ups"), "Strength")),
            currentPlanDay = WorkoutPlanDay(1, 1, "Upper body", listOf("Push-ups"), "Strength"),
            planPosition = PlanPosition(1, 1, 0f),
            isLoading = false,
        )
        return state.copy(dashboard = DashboardUiMapper.map(state, LocalDateTime.of(today, LocalTime.of(10, 0))))
    }

    private fun fakeStateWithImportedMeal(): AestheticState {
        val today = LocalDate.of(2026, 5, 16)
        val habit = HabitEntry(today, waterDone = true, stepsDone = true, proteinDone = false, workoutDone = false, postureDone = false, sleepDone = false)
        val state = fakeState().copy(
            currentHabit = habit,
            habits = listOf(habit),
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, "Яндекс Еда", "Челябинск", null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("lunch", today, "15:00", "Обед", "Белковый обед", "Meal", true, null),
            ),
            importedMealRecommendations = listOf(
                ImportedMealRecommendation(
                    id = "meal",
                    date = today,
                    time = "15:00",
                    type = "lunch",
                    title = "Боул с курицей",
                    restaurant = "Оливер",
                    sourceDescription = null,
                    description = "Курица и овощи",
                    estimatedCalories = 420,
                    estimatedProteinG = 32,
                    estimatedFatG = null,
                    estimatedCarbsG = null,
                    weightG = null,
                    priceRub = 660,
                    foodUrl = "https://example.com",
                    source = "site",
                    fallback = null,
                ),
            ),
        )
        return state.copy(dashboard = DashboardUiMapper.map(state, LocalDateTime.of(today, LocalTime.of(14, 0))))
    }

    private fun fakeStateWithActiveEvent(): AestheticState {
        val today = LocalDate.of(2026, 5, 16)
        val state = fakeState().copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, null, null, null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("current-meal", today, "13:00", "Текущий обед", "Белковый обед", "Meal", true, null),
                ImportedScheduleEvent("next-workout", today, "15:00", "Следующая тренировка", "Домашняя тренировка", "Workout", false, null),
            ),
        )
        return state.copy(dashboard = DashboardUiMapper.map(state, LocalDateTime.of(today, LocalTime.of(13, 5))))
    }

    private fun fakeStateWithStartedActiveEvent(): AestheticState {
        val today = LocalDate.of(2026, 5, 16)
        val state = fakeStateWithActiveEvent().copy(
            scheduleEventStarts = listOf(ScheduleEventStart("current-meal", today)),
        )
        return state.copy(dashboard = DashboardUiMapper.map(state, LocalDateTime.of(today, LocalTime.of(13, 5))))
    }

    private fun fakeStateWithImportedWorkout(): AestheticState {
        val today = LocalDate.of(2026, 5, 16)
        val state = fakeState().copy(
            importedScheduleDays = listOf(ImportedScheduleDay(today, today, "Focus", null, null, null, null, null, emptyList(), emptyList(), emptyList())),
            importedScheduleEvents = listOf(
                ImportedScheduleEvent("workout", today, "18:00", "Upper body", null, "Workout", false, null),
            ),
            importedWorkoutExercises = listOf(
                ImportedWorkoutExercise(
                    id = "exercise-1",
                    date = today,
                    eventId = "workout",
                    workoutId = "upper-body",
                    workoutTitle = "Upper body",
                    workoutDescription = "Домашняя тренировка",
                    workoutEstimatedDurationMin = 30,
                    workoutIntensity = "light",
                    exerciseId = "wall-slides",
                    orderIndex = 0,
                    title = "Wall slides",
                    description = "4x10-12",
                    sets = 4,
                    reps = "10-12",
                    durationSec = null,
                    restSec = 30,
                    rpe = "5",
                    equipment = "none",
                    target = listOf("плечи"),
                    previewImageUrl = null,
                    imageUrls = emptyList(),
                    imageAlt = "Wall slides",
                    sourceUrl = null,
                    techniqueSteps = listOf("Ребра вниз"),
                    commonMistakes = listOf("Не прогибать поясницу"),
                ),
            ),
        )
        return state.copy(dashboard = DashboardUiMapper.map(state, LocalDateTime.of(today, LocalTime.of(17, 0))))
    }
}
