package com.aesthetic.tracker.ui.dashboard

import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.domain.PlanTargets
import org.json.JSONObject
import java.time.LocalDate

object JsonMeasurementImporter {
    private val supportedKeys = setOf(
        "weightKg",
        "weight",
        "bodyFatPercent",
        "bodyFat",
        "skeletalMuscleKg",
        "skeletalMuscle",
        "muscle",
        "pulse",
        "restingPulse",
        "visceralFat",
        "waterPercent",
        "water",
        "bmi",
        "muscleMassKg",
        "muscleMass",
        "proteinPercent",
        "protein",
        "basalMetabolismKcal",
        "bmr",
        "biologicalAge",
        "bioAge",
    )

    fun parse(json: String, date: LocalDate = LocalDate.now(), previous: MeasurementEntry? = null): ImportParseResult {
        if (json.isBlank()) return ImportParseResult.Error("Вставьте JSON с данными замера перед импортом.")
        val normalized = json.trim()
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) {
            return ImportParseResult.Error("JSON должен быть объектом с полями вроде weightKg и bodyFatPercent.")
        }

        val values = runCatching { parseFlatObject(normalized) }
            .getOrElse { return ImportParseResult.Error("Не удалось прочитать JSON. Проверьте запятые, кавычки и числовые значения.") }
        if (values.keys.none { it in supportedKeys }) {
            return ImportParseResult.Error("В JSON не найдены поддерживаемые поля замера.")
        }

        val measurement = MeasurementEntry(
            date = date,
            weightKg = values.double("weightKg", "weight") ?: previous?.weightKg ?: PlanTargets.InitialWeightKg,
            bodyFatPercent = values.double("bodyFatPercent", "bodyFat") ?: previous?.bodyFatPercent ?: PlanTargets.InitialBodyFatPercent,
            skeletalMuscleKg = values.double("skeletalMuscleKg", "skeletalMuscle", "muscle") ?: previous?.skeletalMuscleKg ?: PlanTargets.InitialSkeletalMuscleKg,
            pulse = values.int("pulse", "restingPulse") ?: previous?.pulse ?: PlanTargets.InitialPulse,
            visceralFat = values.int("visceralFat") ?: previous?.visceralFat ?: 0,
            waterPercent = values.double("waterPercent", "water") ?: previous?.waterPercent ?: 0.0,
            bmi = values.double("bmi"),
            muscleMassKg = values.double("muscleMassKg", "muscleMass"),
            proteinPercent = values.double("proteinPercent", "protein"),
            basalMetabolismKcal = values.int("basalMetabolismKcal", "bmr"),
            biologicalAge = values.int("biologicalAge", "bioAge"),
            scalePhotoPath = null,
        )
        return ImportParseResult.Success(measurement)
    }

    private fun parseFlatObject(json: String): Map<String, String> {
        val objectJson = JSONObject(json)
        return objectJson.keys().asSequence().associateWith { key -> objectJson.optString(key) }
    }

    private fun Map<String, String>.double(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key -> this[key]?.toDoubleOrNull() }

    private fun Map<String, String>.int(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key -> this[key]?.toIntOrNull() ?: this[key]?.toDoubleOrNull()?.toInt() }
}

sealed interface ImportParseResult {
    data class Success(val measurement: MeasurementEntry) : ImportParseResult
    data class Error(val message: String) : ImportParseResult
}
