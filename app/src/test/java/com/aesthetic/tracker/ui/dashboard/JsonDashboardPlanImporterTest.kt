package com.aesthetic.tracker.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JsonDashboardPlanImporterTest {
    @Test
    fun `parses imported schedule meals and nested measurement`() {
        val json = """
            {
              "weekStartDate": "2026-05-16",
              "deliveryAddress": "Челябинск",
              "goal": {
                "title": "V-образная сухая форма",
                "visualReference": "Широкие плечи и ровная осанка",
                "targetWeightKg": 69.3,
                "targetBodyFatPercentRange": "14-16",
                "trainingPrinciples": ["Домашние тренировки"],
                "focusMuscles": ["плечи", "широчайшие"],
                "nutritionPrinciples": ["Белок 120-140 г"]
              },
              "foodProvider": {"name":"Яндекс Еда","city":"Челябинск","cityUrl":"https://eda.yandex.ru/chelyabinsk"},
              "measurement": {"weightKg":70.4,"bodyFatPercent":18.0,"skeletalMuscleKg":29.0,"pulse":102},
              "days": [
                {
                  "date": "2026-05-16",
                  "focus": "Белковое питание",
                  "schedule": [
                    {"id":"lunch","time":"15:00","title":"Обед","description":"Белковый обед","kind":"Meal","isFixed":true}
                  ],
                  "meals": [
                    {
                      "id":"lunch-meal",
                      "time":"15:00",
                      "type":"lunch",
                      "title":"Боул с курицей",
                      "restaurant":"Оливер",
                      "description":"Курица, киноа и овощи",
                      "estimatedCalories":420,
                      "estimatedProteinG":32,
                      "priceRub":660,
                      "foodUrl":"https://example.com",
                      "fallback":"Взять курицу с овощами"
                    }
                  ],
                  "nutrition": ["Белок 120 г"],
                  "recovery": [],
                  "checkpoints": []
                }
              ]
            }
        """.trimIndent()

        val result = JsonDashboardPlanImporter.parse(json, LocalDate.of(2026, 5, 16))

        val success = result as DashboardPlanImportResult.Success
        assertEquals(LocalDate.of(2026, 5, 16), success.measurement?.date)
        assertEquals(70.4, success.measurement?.weightKg ?: 0.0, 0.01)
        assertEquals(1, success.days.size)
        assertEquals("Яндекс Еда", success.days.single().foodProviderName)
        assertEquals("Обед", success.events.single().title)
        assertEquals("Боул с курицей", success.meals.single().title)
        assertEquals("https://example.com", success.meals.single().foodUrl)
        assertEquals("current", success.goal?.id)
        assertEquals("V-образная сухая форма", success.goal?.title)
        assertEquals(69.3, success.goal?.targetWeightKg ?: 0.0, 0.01)
        assertEquals(listOf("плечи", "широчайшие"), success.goal?.focusMuscles)
    }

    @Test
    fun `returns not dashboard plan for flat measurement json`() {
        val result = JsonDashboardPlanImporter.parse("""{"weightKg":70.1}""")

        assertEquals(DashboardPlanImportResult.NotDashboardPlan, result)
    }

    @Test
    fun `keeps nullable meal macros null`() {
        val result = JsonDashboardPlanImporter.parse(
            """
            {
              "days": [
                {
                  "date": "2026-05-16",
                  "schedule": [],
                  "meals": [
                    {"id":"meal","time":"20:00","type":"dinner","title":"Шашлычок","estimatedCalories":null}
                  ]
                }
              ]
            }
            """.trimIndent(),
        ) as DashboardPlanImportResult.Success

        assertNull(result.meals.single().estimatedCalories)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `parses workout exercises from training block and library`() {
        val result = JsonDashboardPlanImporter.parse(
            """
            {
              "exerciseLibrary": {
                "wall-slides": {
                  "name": "Wall slides",
                  "previewImageUrl": "https://example.com/wall.png",
                  "imageUrls": ["https://example.com/wall-1.png"],
                  "techniqueSteps": ["Ребра вниз"],
                  "commonMistakes": ["Не прогибать поясницу"]
                }
              },
              "days": [
                {
                  "date": "2026-05-16",
                  "schedule": [
                    {"id":"workout","time":"18:00","title":"Upper body","description":null,"kind":"Workout","isFixed":false}
                  ],
                  "training": {
                    "mainWorkout": {
                      "id": "upper-body",
                      "title": "Upper body",
                      "description": "Домашняя тренировка",
                      "estimatedDurationMin": 30,
                      "intensity": "light",
                      "exercises": [
                        {"exerciseId":"wall-slides","title":"Wall slides","description":"4x10-12","sets":4,"reps":"10-12","restSec":30,"rpe":"5"}
                      ]
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        ) as DashboardPlanImportResult.Success

        val exercise = result.exercises.single()
        assertEquals("2026-05-16-workout", exercise.eventId)
        assertEquals("upper-body", exercise.workoutId)
        assertEquals("Wall slides", exercise.title)
        assertEquals(4, exercise.sets)
        assertEquals("10-12", exercise.reps)
        assertEquals("https://example.com/wall.png", exercise.previewImageUrl)
        assertEquals(listOf("Ребра вниз"), exercise.techniqueSteps)
        assertEquals(listOf("Не прогибать поясницу"), exercise.commonMistakes)
    }

    @Test
    fun `keeps workout event when exercises are partially invalid`() {
        val result = JsonDashboardPlanImporter.parse(
            """
            {
              "days": [
                {
                  "date": "2026-05-16",
                  "schedule": [
                    {
                      "id":"workout",
                      "time":"18:00",
                      "title":"Upper body",
                      "kind":"Workout",
                      "exercises": [
                        {"sets":4},
                        {"name":"Rows","sets":3,"reps":"10","restSeconds":90,"note":"tempo 3-1-1"}
                      ]
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        ) as DashboardPlanImportResult.Success

        assertEquals("Upper body", result.events.single().title)
        assertEquals(1, result.exercises.size)
        assertEquals("Rows", result.exercises.single().title)
        assertEquals(90, result.exercises.single().restSec)
        assertEquals("tempo 3-1-1", result.exercises.single().description)
    }

    @Test
    fun `scopes repeated ids by date`() {
        val result = JsonDashboardPlanImporter.parse(
            """
            {
              "days": [
                {
                  "date": "2026-05-16",
                  "schedule": [
                    {"id":"lunch","time":"15:00","title":"Обед","kind":"Meal"}
                  ],
                  "meals": [
                    {"id":"lunch-meal","time":"15:00","type":"lunch","title":"Боул"}
                  ]
                },
                {
                  "date": "2026-05-17",
                  "schedule": [
                    {"id":"lunch","time":"15:00","title":"Обед","kind":"Meal"}
                  ],
                  "meals": [
                    {"id":"lunch-meal","time":"15:00","type":"lunch","title":"Салат"}
                  ]
                }
              ]
            }
            """.trimIndent(),
        ) as DashboardPlanImportResult.Success

        assertEquals(listOf("2026-05-16-lunch", "2026-05-17-lunch"), result.events.map { it.id })
        assertEquals(listOf("2026-05-16-lunch-meal", "2026-05-17-lunch-meal"), result.meals.map { it.id })
    }

    @Test
    fun `parses common exercise catalog and training array`() {
        val result = JsonDashboardPlanImporter.parse(
            """
            {
              "exerciseCatalog": [
                {
                  "id": "wall-slides",
                  "name": "Wall slides",
                  "description": "Catalog description",
                  "equipment": "none",
                  "targetMuscles": ["плечи", "осанка"],
                  "previewImageUrl": "https://example.com/wall.png",
                  "imageUrls": ["https://example.com/wall-1.png", "https://example.com/wall-2.png"],
                  "techniqueSteps": ["Ребра вниз"],
                  "commonMistakes": ["Не прогибать поясницу"]
                },
                {
                  "id": "posture-wall-routine",
                  "name": "Осанка у стены",
                  "targetMuscles": ["осанка"],
                  "previewImageUrl": "https://example.com/posture.png"
                }
              ],
              "days": [
                {
                  "date": "2026-05-16",
                  "schedule": [
                    {"id":"2026-05-16-workout-v-shape-home","time":"17:30","title":"Дом V-форма A","kind":"Workout"},
                    {"id":"2026-05-16-posture-wall","time":"22:30","title":"Осанка у стены","kind":"Workout"}
                  ],
                  "training": [
                    {
                      "id": "main-v-shape-a",
                      "time": "17:30",
                      "name": "Дом V-форма A",
                      "description": "20 минут",
                      "estimatedDurationMin": 20,
                      "intensity": "light",
                      "exercises": [
                        {"exerciseId":"wall-slides","name":"Wall slides","sets":4,"reps":"10-12","restSec":30,"rpe":"5"}
                      ]
                    },
                    {
                      "id": "evening-posture-a",
                      "time": "22:30",
                      "name": "Осанка у стены",
                      "exercises": [
                        {"exerciseId":"posture-wall-routine","name":"Осанка у стены","durationSec":600,"restSec":20,"rpe":"3"}
                      ]
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        ) as DashboardPlanImportResult.Success

        val wallSlides = result.exercises.first { it.exerciseId == "wall-slides" }
        val posture = result.exercises.first { it.exerciseId == "posture-wall-routine" }

        assertEquals(2, result.exercises.size)
        assertEquals("2026-05-16-workout-v-shape-home", wallSlides.eventId)
        assertEquals("main-v-shape-a", wallSlides.workoutId)
        assertEquals("Дом V-форма A", wallSlides.workoutTitle)
        assertEquals(listOf("плечи", "осанка"), wallSlides.target)
        assertEquals("https://example.com/wall.png", wallSlides.previewImageUrl)
        assertEquals(listOf("Ребра вниз"), wallSlides.techniqueSteps)
        assertEquals(listOf("Не прогибать поясницу"), wallSlides.commonMistakes)
        assertEquals("2026-05-16-posture-wall", posture.eventId)
        assertEquals(null, posture.sets)
        assertEquals(null, posture.reps)
        assertEquals(600, posture.durationSec)
    }
}
