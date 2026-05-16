package com.aesthetic.tracker.data

import com.aesthetic.tracker.domain.GeneratedTodayPlan
import com.aesthetic.tracker.domain.PlanTargets
import com.aesthetic.tracker.domain.TodayPlanInput
import com.aesthetic.tracker.domain.TodayScheduleItem
import com.aesthetic.tracker.domain.TodayScheduleKind
import com.aesthetic.tracker.domain.TodayTrainingItem
import com.aesthetic.tracker.domain.buildTodayPlanPrompt
import com.aesthetic.tracker.domain.signature
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayPlanFileParser @Inject constructor() {
    fun parse(content: String, input: TodayPlanInput): Result<TodayPlanImport> = runCatching {
        val body = JSONObject(content.stripJsonFence())
        val measurement = body.findMeasurement(input)
        val effectiveInput = measurement?.let { imported ->
            input.copy(measurementsDescending = listOf(imported) + input.measurementsDescending.filterNot { it.date == imported.date })
        } ?: input
        val dayBody = body.optJSONArray("days")?.selectDay(input.today) ?: body
        TodayPlanImport(
            plan = GeneratedTodayPlan(
                signature = effectiveInput.signature(),
                focus = dayBody.getString("focus"),
                schedule = dayBody.getJSONArray("schedule").toScheduleItems(),
                training = dayBody.getJSONArray("training").toTrainingItems(),
                nutrition = dayBody.getJSONArray("nutrition").toStringList(),
                recovery = dayBody.getJSONArray("recovery").toStringList(),
                checkpoints = dayBody.getJSONArray("checkpoints").toStringList(),
                prompt = buildTodayPlanPrompt(effectiveInput),
            ),
            measurement = measurement,
        )
    }

    private fun JSONObject.findMeasurement(input: TodayPlanInput): MeasurementEntry? {
        val body = optJSONObject("measurement")
            ?: optJSONObject("userInfo")
            ?: optJSONObject("bodyMetrics")
            ?: return null
        val latest = input.measurementsDescending.firstOrNull()
        return MeasurementEntry(
            date = body.localDate("date") ?: input.today,
            bodyScore = body.intValue("bodyScore", "score"),
            weightKg = body.doubleValue("weightKg", "weight") ?: latest?.weightKg ?: PlanTargets.InitialWeightKg,
            bodyFatPercent = body.doubleValue("bodyFatPercent", "bodyFat") ?: latest?.bodyFatPercent ?: PlanTargets.InitialBodyFatPercent,
            fatMassKg = body.doubleValue("fatMassKg", "fatKg"),
            skeletalMuscleKg = body.doubleValue("skeletalMuscleKg", "skeletalMuscle") ?: latest?.skeletalMuscleKg ?: PlanTargets.InitialSkeletalMuscleKg,
            muscleMassKg = body.doubleValue("muscleMassKg", "muscleMass"),
            muscleRatePercent = body.doubleValue("muscleRatePercent", "muscleRate"),
            pulse = body.intValue("pulse", "heartRate") ?: latest?.pulse ?: PlanTargets.InitialPulse,
            visceralFat = body.intValue("visceralFat", "visceralFatLevel") ?: latest?.visceralFat ?: 0,
            waterPercent = body.doubleValue("waterPercent", "bodyWaterPercent", "water") ?: latest?.waterPercent ?: 0.0,
            bodyWaterKg = body.doubleValue("bodyWaterKg", "bodyWaterMassKg"),
            bmi = body.doubleValue("bmi"),
            mineralMassKg = body.doubleValue("mineralMassKg", "mineralsKg"),
            proteinMassKg = body.doubleValue("proteinMassKg", "proteinKg"),
            proteinPercent = body.doubleValue("proteinPercent", "protein"),
            subcutaneousFatPercent = body.doubleValue("subcutaneousFatPercent", "subcutaneousFat"),
            leanBodyMassKg = body.doubleValue("leanBodyMassKg", "fatFreeMassKg"),
            basalMetabolismKcal = body.intValue("basalMetabolismKcal", "bmrKcal", "bmr"),
            biologicalAge = body.intValue("biologicalAge", "bodyAge"),
            bodyType = body.stringValue("bodyType"),
            standardWeightKg = body.doubleValue("standardWeightKg"),
            weightControlKg = body.doubleValue("weightControlKg"),
            fatControlKg = body.doubleValue("fatControlKg"),
            muscleControlKg = body.doubleValue("muscleControlKg"),
        )
    }

    private fun JSONArray.selectDay(today: LocalDate): JSONObject {
        val days = (0 until length()).map { getJSONObject(it) }
        return days.firstOrNull { it.optString("date") == today.toString() }
            ?: days.minByOrNull { day ->
                runCatching { kotlin.math.abs(LocalDate.parse(day.optString("date")).toEpochDay() - today.toEpochDay()) }
                    .getOrDefault(Long.MAX_VALUE)
            }
            ?: error("Weekly schedule does not contain days")
    }

    private fun JSONArray.toScheduleItems(): List<TodayScheduleItem> =
        (0 until length()).map { index ->
            val item = getJSONObject(index)
            TodayScheduleItem(
                id = item.getString("id"),
                time = LocalTime.parse(item.getString("time")),
                title = item.getString("title"),
                description = item.getString("description"),
                kind = TodayScheduleKind.valueOf(item.getString("kind")),
                isFixed = item.getBoolean("isFixed"),
                notificationText = item.getString("notificationText"),
            )
        }.sortedBy { it.time }

    private fun JSONArray.toTrainingItems(): List<TodayTrainingItem> =
        (0 until length()).map { index ->
            when (val item = get(index)) {
                is JSONObject -> TodayTrainingItem(
                    title = item.stringValue("title") ?: item.stringValue("exercise") ?: item.stringValue("name") ?: "Тренировка",
                    description = item.stringValue("description") ?: item.stringValue("details") ?: item.stringValue("notes") ?: "",
                )
                is String -> TodayTrainingItem(
                    title = item,
                    description = "",
                )
                else -> TodayTrainingItem(
                    title = item.toString(),
                    description = "",
                )
            }
        }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun JSONObject.localDate(key: String): LocalDate? =
        stringValue(key)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun JSONObject.doubleValue(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key ->
            if (!has(key) || isNull(key)) {
                null
            } else {
                when (val value = opt(key)) {
                    is Number -> value.toDouble()
                    is String -> value.toMetricDouble()
                    else -> null
                }
            }
        }

    private fun JSONObject.intValue(vararg keys: String): Int? =
        doubleValue(*keys)?.toInt()

    private fun JSONObject.stringValue(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).trim().takeIf { it.isNotBlank() } else null

    private fun String.toMetricDouble(): Double? =
        replace(',', '.')
            .replace(Regex("[^0-9.+-]"), "")
            .takeIf { it.isNotBlank() && it != "+" && it != "-" && it != "." }
            ?.toDoubleOrNull()

    private fun String.stripJsonFence(): String =
        trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
}

data class TodayPlanImport(
    val plan: GeneratedTodayPlan,
    val measurement: MeasurementEntry?,
)
