package com.aesthetic.tracker.ui.dashboard

import com.aesthetic.tracker.data.ImportedMealRecommendation
import com.aesthetic.tracker.data.ImportedGoal
import com.aesthetic.tracker.data.ImportedScheduleDay
import com.aesthetic.tracker.data.ImportedScheduleEvent
import com.aesthetic.tracker.data.ImportedWorkoutExercise
import com.aesthetic.tracker.data.MeasurementEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object JsonDashboardPlanImporter {
    fun parse(json: String, importDate: LocalDate = LocalDate.now(), previous: MeasurementEntry? = null): DashboardPlanImportResult {
        if (json.isBlank()) return DashboardPlanImportResult.Error("Вставьте JSON с расписанием перед импортом.")
        val root = runCatching { JSONObject(json.trim()) }
            .getOrElse { return DashboardPlanImportResult.Error("Не удалось прочитать JSON расписания. Проверьте структуру файла.") }

        if (!root.has("days")) return DashboardPlanImportResult.NotDashboardPlan

        val daysArray = root.optJSONArray("days")
            ?: return DashboardPlanImportResult.Error("В JSON расписания не найден массив days.")
        if (daysArray.length() == 0) return DashboardPlanImportResult.Error("В JSON расписания нет дней для импорта.")

        val weekStart = root.optNullableDate("weekStartDate")
        val provider = root.optJSONObject("foodProvider")
        val goal = root.optJSONObject("goal")?.let(::parseGoal)
        val exerciseCatalog = root.exerciseCatalog()
        val importedDays = mutableListOf<ImportedScheduleDay>()
        val importedEvents = mutableListOf<ImportedScheduleEvent>()
        val importedMeals = mutableListOf<ImportedMealRecommendation>()
        val importedExercises = mutableListOf<ImportedWorkoutExercise>()

        for (index in 0 until daysArray.length()) {
            val dayJson = daysArray.optJSONObject(index) ?: continue
            val date = dayJson.optNullableDate("date") ?: continue
            importedDays += ImportedScheduleDay(
                date = date,
                weekStartDate = weekStart,
                focus = dayJson.optNullableString("focus"),
                deliveryAddress = root.optNullableString("deliveryAddress"),
                foodProviderName = provider?.optNullableString("name"),
                foodProviderCity = provider?.optNullableString("city"),
                foodProviderCityUrl = provider?.optNullableString("cityUrl"),
                foodProviderNote = provider?.optNullableString("note"),
                nutrition = dayJson.optStringList("nutrition"),
                recovery = dayJson.optStringList("recovery"),
                checkpoints = dayJson.optStringList("checkpoints"),
            )
            val dayEvents = parseEvents(dayJson.optJSONArray("schedule"), date)
            importedEvents += dayEvents
            importedMeals += parseMeals(dayJson.optJSONArray("meals"), date)
            importedExercises += parseWorkoutExercises(
                dayJson = dayJson,
                date = date,
                events = dayEvents,
                exerciseCatalog = exerciseCatalog,
            )
        }

        if (importedDays.isEmpty()) return DashboardPlanImportResult.Error("В JSON расписания нет дней с корректной датой.")

        val measurementJson = root.optJSONObject("measurement")
        val measurementDate = measurementJson?.optNullableDate("date") ?: importDate
        val measurement = measurementJson
            ?.let { JsonMeasurementImporter.parse(it.toString(), measurementDate, previous) }
            ?.let { result -> (result as? ImportParseResult.Success)?.measurement }

        return DashboardPlanImportResult.Success(
            measurement = measurement,
            days = importedDays,
            events = importedEvents,
            meals = importedMeals,
            exercises = importedExercises,
            goal = goal,
        )
    }

    private fun parseGoal(item: JSONObject): ImportedGoal? {
        val title = item.optNullableString("title") ?: return null
        return ImportedGoal(
            id = "current",
            title = title,
            visualReference = item.optNullableString("visualReference"),
            targetWeightKg = item.optNullableDouble("targetWeightKg"),
            targetBodyFatPercentRange = item.optNullableString("targetBodyFatPercentRange"),
            trainingPrinciples = item.optStringList("trainingPrinciples"),
            focusMuscles = item.optStringList("focusMuscles"),
            nutritionPrinciples = item.optStringList("nutritionPrinciples"),
        )
    }

    private fun parseEvents(array: JSONArray?, date: LocalDate): List<ImportedScheduleEvent> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val time = item.optNullableString("time") ?: return@mapNotNull null
            val title = item.optNullableString("title") ?: return@mapNotNull null
            val rawId = item.optNullableString("id") ?: "$time-$title"
            ImportedScheduleEvent(
                id = scopedImportId(date, rawId),
                date = date,
                time = time,
                title = title,
                description = item.optNullableString("description"),
                kind = item.optNullableString("kind") ?: "Other",
                isFixed = item.optBoolean("isFixed", false),
                notificationText = item.optNullableString("notificationText"),
            )
        }
    }

    private fun parseMeals(array: JSONArray?, date: LocalDate): List<ImportedMealRecommendation> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val time = item.optNullableString("time") ?: return@mapNotNull null
            val type = item.optNullableString("type") ?: return@mapNotNull null
            val title = item.optNullableString("title") ?: return@mapNotNull null
            val rawId = item.optNullableString("id") ?: "$time-$type"
            ImportedMealRecommendation(
                id = scopedImportId(date, rawId),
                date = date,
                time = time,
                type = type,
                title = title,
                restaurant = item.optNullableString("restaurant"),
                sourceDescription = item.optNullableString("sourceDescription"),
                description = item.optNullableString("description"),
                estimatedCalories = item.optNullableInt("estimatedCalories"),
                estimatedProteinG = item.optNullableInt("estimatedProteinG"),
                estimatedFatG = item.optNullableInt("estimatedFatG"),
                estimatedCarbsG = item.optNullableInt("estimatedCarbsG"),
                weightG = item.optNullableInt("weightG"),
                priceRub = item.optNullableInt("priceRub"),
                foodUrl = item.optNullableString("foodUrl"),
                source = item.optNullableString("source"),
                fallback = item.optNullableString("fallback"),
            )
        }
    }

    private fun parseWorkoutExercises(
        dayJson: JSONObject,
        date: LocalDate,
        events: List<ImportedScheduleEvent>,
        exerciseCatalog: Map<String, JSONObject>,
    ): List<ImportedWorkoutExercise> {
        val fromSchedule = events.flatMap { event ->
            val source = dayJson.optJSONArray("schedule")
                ?.findObject { scheduleItem ->
                    val time = scheduleItem.optNullableString("time")
                    val title = scheduleItem.optNullableString("title")
                    val rawId = scheduleItem.optNullableString("id")
                        ?: listOfNotNull(time, title).joinToString("-").takeIf { it.isNotBlank() }
                    rawId != null && scopedImportId(date, rawId) == event.id
                }
                ?.optJSONArray("exercises")
            parseExerciseArray(
                array = source,
                date = date,
                event = event,
                workout = null,
                exerciseCatalog = exerciseCatalog,
            )
        }
        val training = dayJson.opt("training")
        val workouts = when (training) {
            is JSONArray -> (0 until training.length()).mapNotNull { training.optJSONObject(it) }
            is JSONObject -> listOf("mainWorkout", "postureWorkout").mapNotNull { key -> training.optJSONObject(key) }
            else -> emptyList()
        }
        val fromTraining = workouts.flatMap { workout ->
            val event = events.findWorkoutEvent(workout)
            parseExerciseArray(
                array = workout.optJSONArray("exercises"),
                date = date,
                event = event,
                workout = workout,
                exerciseCatalog = exerciseCatalog,
            )
        }
        return fromSchedule + fromTraining
    }

    private fun parseExerciseArray(
        array: JSONArray?,
        date: LocalDate,
        event: ImportedScheduleEvent?,
        workout: JSONObject?,
        exerciseCatalog: Map<String, JSONObject>,
    ): List<ImportedWorkoutExercise> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val exerciseId = item.optNullableString("exerciseId")
                ?: item.optNullableString("id")
                ?: item.optNullableString("name")
                ?: item.optNullableString("title")
                ?: return@mapNotNull null
            val library = exerciseCatalog[exerciseId]
            val title = item.optNullableString("title")
                ?: item.optNullableString("name")
                ?: library?.optNullableString("name")
                ?: return@mapNotNull null
            ImportedWorkoutExercise(
                id = "${date}-${event?.id ?: workout?.optNullableString("id") ?: "workout"}-$index-$exerciseId",
                date = date,
                eventId = event?.id,
                workoutId = workout?.optNullableString("id"),
                workoutTitle = workout?.optNullableString("title") ?: workout?.optNullableString("name") ?: event?.title,
                workoutDescription = workout?.optNullableString("description"),
                workoutEstimatedDurationMin = workout?.optNullableInt("estimatedDurationMin"),
                workoutIntensity = workout?.optNullableString("intensity"),
                exerciseId = exerciseId,
                orderIndex = index,
                title = title,
                description = item.optNullableString("description") ?: item.optNullableString("note") ?: library?.optNullableString("description"),
                sets = item.optNullableInt("sets"),
                reps = item.optNullableString("reps"),
                durationSec = item.optNullableInt("durationSec") ?: item.optNullableInt("durationSeconds"),
                restSec = item.optNullableInt("restSec") ?: item.optNullableInt("restSeconds"),
                rpe = item.optNullableString("rpe"),
                equipment = item.optNullableString("equipment") ?: library?.optNullableString("equipment"),
                target = item.optStringList("target")
                    .ifEmpty { item.optStringList("targetMuscles") }
                    .ifEmpty { library?.optStringList("target").orEmpty() }
                    .ifEmpty { library?.optStringList("targetMuscles").orEmpty() },
                previewImageUrl = item.optNullableString("previewImageUrl") ?: library?.optNullableString("previewImageUrl"),
                imageUrls = item.optStringList("imageUrls").ifEmpty { library?.optStringList("imageUrls").orEmpty() },
                imageAlt = item.optNullableString("imageAlt") ?: library?.optNullableString("imageAlt"),
                sourceUrl = item.optNullableString("sourceUrl") ?: library?.optNullableString("sourceUrl"),
                techniqueSteps = item.optStringList("techniqueSteps").ifEmpty { library?.optStringList("techniqueSteps").orEmpty() },
                commonMistakes = item.optStringList("commonMistakes").ifEmpty { library?.optStringList("commonMistakes").orEmpty() },
            )
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) optString(key).toDoubleOrNull()?.toInt() else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) optString(key).toDoubleOrNull() else null

    private fun JSONObject.optNullableDate(key: String): LocalDate? =
        optNullableString(key)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun JSONArray.findObject(predicate: (JSONObject) -> Boolean): JSONObject? {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            if (predicate(item)) return item
        }
        return null
    }

    private fun scopedImportId(date: LocalDate, rawId: String): String =
        if (rawId.startsWith("$date-")) rawId else "$date-$rawId"

    private fun List<ImportedScheduleEvent>.findWorkoutEvent(workout: JSONObject): ImportedScheduleEvent? {
        val title = workout.optNullableString("title") ?: workout.optNullableString("name")
        val time = workout.optNullableString("time")
        return firstOrNull { it.kind.equals("Workout", ignoreCase = true) && !time.isNullOrBlank() && it.time == time }
            ?: firstOrNull { it.kind.equals("Workout", ignoreCase = true) && !title.isNullOrBlank() && it.title == title }
            ?: firstOrNull { it.kind.equals("Workout", ignoreCase = true) }
    }

    private fun JSONObject.exerciseCatalog(): Map<String, JSONObject> {
        val catalog = linkedMapOf<String, JSONObject>()
        optJSONObject("exerciseLibrary")?.let { library ->
            library.keys().forEach { key ->
                library.optJSONObject(key)?.let { catalog[key] = it }
            }
        }
        optJSONObject("exerciseCatalog")?.let { library ->
            library.keys().forEach { key ->
                library.optJSONObject(key)?.let { catalog[key] = it }
            }
        }
        optJSONArray("exerciseCatalog")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optNullableString("id") ?: continue
                catalog[id] = item
            }
        }
        return catalog
    }
}

sealed interface DashboardPlanImportResult {
    data object NotDashboardPlan : DashboardPlanImportResult
    data class Success(
        val measurement: MeasurementEntry?,
        val days: List<ImportedScheduleDay>,
        val events: List<ImportedScheduleEvent>,
        val meals: List<ImportedMealRecommendation>,
        val exercises: List<ImportedWorkoutExercise>,
        val goal: ImportedGoal?,
    ) : DashboardPlanImportResult
    data class Error(val message: String) : DashboardPlanImportResult
}
