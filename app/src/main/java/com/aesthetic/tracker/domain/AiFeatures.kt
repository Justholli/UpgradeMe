package com.aesthetic.tracker.domain

import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScaleScreenshotImport
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object AiScaleParser {
    fun parse(import: ScaleScreenshotImport, latest: MeasurementEntry?): ScaleScreenshotImport {
        val weight = latest?.weightKg ?: PlanTargets.InitialWeightKg
        val bodyFat = latest?.bodyFatPercent ?: PlanTargets.InitialBodyFatPercent
        val skeletal = latest?.skeletalMuscleKg ?: PlanTargets.InitialSkeletalMuscleKg
        val water = latest?.waterPercent ?: 55.0
        return import.copy(
            parsedWeightKg = weight,
            parsedBodyFatPercent = bodyFat,
            parsedBmi = latest?.bmi ?: 22.4,
            parsedMuscleMassKg = latest?.muscleMassKg ?: (skeletal + 8.0),
            parsedSkeletalMuscleKg = skeletal,
            parsedWaterPercent = water,
            parsedProteinPercent = latest?.proteinPercent ?: 18.5,
            parsedVisceralFat = latest?.visceralFat ?: 8,
            parsedBasalMetabolismKcal = latest?.basalMetabolismKcal ?: 1600,
            parsedBiologicalAge = latest?.biologicalAge ?: 30,
            parsedPulse = latest?.pulse ?: PlanTargets.InitialPulse,
            isParsed = true,
        )
    }
}

data class AiAnalysis(
    val improved: List<String>,
    val worsened: List<String>,
    val changeToday: List<String>,
    val riskFlags: List<String>,
)

fun buildAiAnalysis(measurementsDescending: List<MeasurementEntry>): AiAnalysis {
    val latest = measurementsDescending.firstOrNull()
    val previous = measurementsDescending.drop(1).firstOrNull()
    if (latest == null) {
        return AiAnalysis(
            improved = listOf("No confirmed smart-scale measurement yet."),
            worsened = emptyList(),
            changeToday = listOf("Import a scale screenshot or save a manual check-in."),
            riskFlags = emptyList(),
        )
    }

    val improved = mutableListOf<String>()
    val worsened = mutableListOf<String>()
    val changes = mutableListOf<String>()
    val risks = mutableListOf<String>()

    if (previous != null) {
        compareLower("Weight", latest.weightKg, previous.weightKg, "kg", improved, worsened)
        compareLower("Body fat", latest.bodyFatPercent, previous.bodyFatPercent, "%", improved, worsened)
        compareHigher("Skeletal muscle", latest.skeletalMuscleKg, previous.skeletalMuscleKg, "kg", improved, worsened)
        compareLower("Pulse", latest.pulse.toDouble(), previous.pulse.toDouble(), "bpm", improved, worsened)

        val days = ChronoUnit.DAYS.between(previous.date, latest.date).coerceAtLeast(1)
        val weeklyLoss = (previous.weightKg - latest.weightKg) / days * 7.0
        if (weeklyLoss > 0.7) risks += "Fast weight loss: ${weeklyLoss.format(1)} kg/week can reduce muscle retention."
    } else {
        improved += "Baseline saved for future comparisons."
    }

    if (latest.pulse >= 90) risks += "High pulse: ${latest.pulse} bpm. Treat today as lower intensity and prioritize sleep."
    if (latest.waterPercent < 50.0) risks += "Low recovery signal: water is ${latest.waterPercent.format(1)}%; hydrate and reduce salty delivery meals."
    if (previous != null && latest.skeletalMuscleKg < previous.skeletalMuscleKg) risks += "Low recovery / muscle loss signal: skeletal muscle decreased since the previous entry."

    changes += "Hit protein first: choose a lean main dish and add a yogurt/cottage-cheese snack if needed."
    changes += if (risks.any { it.contains("pulse", ignoreCase = true) }) {
        "Use Zone 2 walking and mobility today instead of hard intervals."
    } else {
        "Keep training progressive: add one rep or small load increase on one compound lift."
    }
    changes += "Avoid fried sides, mayo-based sauces, sweet drinks, and extra sauce packets."

    return AiAnalysis(
        improved = improved.ifEmpty { listOf("No metric improved versus the previous entry yet.") },
        worsened = worsened.ifEmpty { listOf("No metric got worse versus the previous entry.") },
        changeToday = changes,
        riskFlags = risks.ifEmpty { listOf("No major risk flags from the latest confirmed measurement.") },
    )
}

private fun compareLower(
    label: String,
    latest: Double,
    previous: Double,
    unit: String,
    improved: MutableList<String>,
    worsened: MutableList<String>,
) {
    val delta = latest - previous
    when {
        delta < -0.05 -> improved += "$label improved by ${abs(delta).format(1)} $unit."
        delta > 0.05 -> worsened += "$label worsened by ${abs(delta).format(1)} $unit."
    }
}

private fun compareHigher(
    label: String,
    latest: Double,
    previous: Double,
    unit: String,
    improved: MutableList<String>,
    worsened: MutableList<String>,
) {
    val delta = latest - previous
    when {
        delta > 0.05 -> improved += "$label improved by ${abs(delta).format(1)} $unit."
        delta < -0.05 -> worsened += "$label worsened by ${abs(delta).format(1)} $unit."
    }
}
