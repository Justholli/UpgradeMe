package com.aesthetic.tracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AestheticRepositoryTest {
    private var database: AppDatabase? = null
    private lateinit var repository: AestheticRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = AestheticRepository(requireNotNull(database).aestheticDao())
    }

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun `replace imported schedule replaces only matching dates`() = runTest {
        val firstDate = LocalDate.of(2026, 5, 16)
        val secondDate = LocalDate.of(2026, 5, 17)
        repository.replaceImportedSchedule(
            days = listOf(importedDay(firstDate, "First"), importedDay(secondDate, "Second")),
            events = listOf(importedEvent("first-old", firstDate, "Old first"), importedEvent("second-old", secondDate, "Old second")),
            meals = emptyList(),
            exercises = emptyList(),
            goal = null,
        )
        repository.startScheduleEvent(ScheduleEventStart("first-old", firstDate))
        repository.completeScheduleEvent(ScheduleEventCompletion("first-old", firstDate))
        repository.startScheduleEvent(ScheduleEventStart("second-old", secondDate))
        repository.completeScheduleEvent(ScheduleEventCompletion("second-old", secondDate))

        repository.replaceImportedSchedule(
            days = listOf(importedDay(firstDate, "First updated")),
            events = listOf(importedEvent("first-new", firstDate, "New first")),
            meals = emptyList(),
            exercises = listOf(importedExercise(firstDate, "first-new")),
            goal = null,
        )

        val days = repository.importedScheduleDays.first()
        val events = repository.importedScheduleEvents.first()
        val exercises = repository.importedWorkoutExercises.first()
        val starts = repository.scheduleEventStarts.first()
        val completions = repository.scheduleEventCompletions.first()

        assertEquals(listOf(firstDate, secondDate), days.map { it.date })
        assertEquals("First updated", days.first { it.date == firstDate }.focus)
        assertEquals("Second", days.first { it.date == secondDate }.focus)
        assertEquals(listOf("first-new", "second-old"), events.map { it.id })
        assertEquals("first-new", exercises.single().eventId)
        assertEquals(listOf("second-old"), starts.map { it.eventId })
        assertEquals(listOf("second-old"), completions.map { it.eventId })
    }

    @Test
    fun `clear imported schedule removes exercises and goal`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        repository.replaceImportedSchedule(
            days = listOf(importedDay(date, "Focus")),
            events = listOf(importedEvent("workout", date, "Workout")),
            meals = emptyList(),
            exercises = listOf(importedExercise(date, "workout")),
            goal = ImportedGoal(
                id = "current",
                title = "Goal",
                visualReference = null,
                targetWeightKg = null,
                targetBodyFatPercentRange = null,
                trainingPrinciples = emptyList(),
                focusMuscles = emptyList(),
                nutritionPrinciples = emptyList(),
            ),
        )
        repository.startScheduleEvent(ScheduleEventStart("workout", date))
        repository.completeScheduleEvent(ScheduleEventCompletion("workout", date))

        repository.clearImportedSchedule()

        assertEquals(emptyList<ImportedScheduleDay>(), repository.importedScheduleDays.first())
        assertEquals(emptyList<ImportedWorkoutExercise>(), repository.importedWorkoutExercises.first())
        assertEquals(emptyList<ScheduleEventStart>(), repository.scheduleEventStarts.first())
        assertEquals(emptyList<ScheduleEventCompletion>(), repository.scheduleEventCompletions.first())
        assertEquals(null, repository.importedGoal.first())
    }

    private fun importedDay(date: LocalDate, focus: String): ImportedScheduleDay =
        ImportedScheduleDay(date, date, focus, null, null, null, null, null, emptyList(), emptyList(), emptyList())

    private fun importedEvent(id: String, date: LocalDate, title: String): ImportedScheduleEvent =
        ImportedScheduleEvent(id, date, "18:00", title, null, "Workout", false, null)

    private fun importedExercise(date: LocalDate, eventId: String): ImportedWorkoutExercise =
        ImportedWorkoutExercise(
            id = "$eventId-exercise",
            date = date,
            eventId = eventId,
            workoutId = "workout",
            workoutTitle = "Workout",
            workoutDescription = null,
            workoutEstimatedDurationMin = null,
            workoutIntensity = null,
            exerciseId = "wall-slides",
            orderIndex = 0,
            title = "Wall slides",
            description = null,
            sets = 4,
            reps = "10-12",
            durationSec = null,
            restSec = 30,
            rpe = "5",
            equipment = null,
            target = emptyList(),
            previewImageUrl = null,
            imageUrls = emptyList(),
            imageAlt = null,
            sourceUrl = null,
            techniqueSteps = emptyList(),
            commonMistakes = emptyList(),
        )
}
