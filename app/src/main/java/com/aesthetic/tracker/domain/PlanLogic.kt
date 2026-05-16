package com.aesthetic.tracker.domain

import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.RecommendationPriority
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil

object PlanTargets {
    const val InitialWeightKg = 70.4
    const val InitialBodyFatPercent = 18.0
    const val InitialSkeletalMuscleKg = 29.0
    const val InitialPulse = 102
    const val TargetMinWeightKg = 67.0
    const val TargetMaxWeightKg = 69.0
    const val TargetMinBodyFatPercent = 13.0
    const val TargetMaxBodyFatPercent = 15.0
    const val TargetMuscleGainMinKg = 1.0
    const val TargetMuscleGainMaxKg = 2.0
    const val PlanLengthWeeks = 12
}

data class PlanPosition(val week: Int, val day: Int, val percentComplete: Float)

data class ProgressSnapshot(
    val weightChangeKg: Double,
    val bodyFatChangePercent: Double,
    val skeletalMuscleChangeKg: Double,
    val pulseChange: Int,
    val averageWeeklyWeightChangeKg: Double,
)

fun currentPlanPosition(startDate: LocalDate, today: LocalDate = LocalDate.now()): PlanPosition {
    val elapsedDays = ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(0).toInt()
    val week = (elapsedDays / 7 + 1).coerceIn(1, PlanTargets.PlanLengthWeeks)
    val day = (elapsedDays % 7 + 1).coerceIn(1, 7)
    val percent = (elapsedDays.toFloat() / (PlanTargets.PlanLengthWeeks * 7)).coerceIn(0f, 1f)
    return PlanPosition(week, day, percent)
}

fun progressFor(latest: MeasurementEntry?, planStartDate: LocalDate): ProgressSnapshot {
    val measurement = latest ?: return ProgressSnapshot(0.0, 0.0, 0.0, 0, 0.0)
    val elapsedWeeks = ceil(ChronoUnit.DAYS.between(planStartDate, measurement.date).coerceAtLeast(1) / 7.0).coerceAtLeast(1.0)
    val weightChange = measurement.weightKg - PlanTargets.InitialWeightKg
    return ProgressSnapshot(
        weightChangeKg = weightChange,
        bodyFatChangePercent = measurement.bodyFatPercent - PlanTargets.InitialBodyFatPercent,
        skeletalMuscleChangeKg = measurement.skeletalMuscleKg - PlanTargets.InitialSkeletalMuscleKg,
        pulseChange = measurement.pulse - PlanTargets.InitialPulse,
        averageWeeklyWeightChangeKg = weightChange / elapsedWeeks,
    )
}

fun buildRecommendations(measurementsDescending: List<MeasurementEntry>, planStartDate: LocalDate): List<Recommendation> {
    val latest = measurementsDescending.firstOrNull()
    val previous = measurementsDescending.drop(1).firstOrNull()
    val progress = progressFor(latest, planStartDate)
    val recommendations = mutableListOf<Recommendation>()

    if (latest != null && latest.pulse > 90) {
        recommendations += Recommendation(
            title = "Снизьте пульс покоя",
            description = "Сделайте приоритетом 7,5-9 часов сна, ежедневную ходьбу, носовое дыхание и снизьте интенсивность тренировок, пока пульс не уйдет ниже 90 уд/мин.",
            priority = RecommendationPriority.High,
        )
    }

    if (progress.averageWeeklyWeightChangeKg < -0.7) {
        recommendations += Recommendation(
            title = "Слегка замедлите дефицит",
            description = "Среднее снижение: ${abs(progress.averageWeeklyWeightChangeKg).format(1)} кг/нед. Добавьте 150-250 ккал/день или уменьшите кардио, чтобы защитить мышцы.",
            priority = RecommendationPriority.High,
        )
    }

    if (bodyFatStalledForTwoWeeks(measurementsDescending)) {
        recommendations += Recommendation(
            title = "Верните динамику снижения жира",
            description = "Процент жира не снижается около двух недель. Добавьте 1500-2000 шагов в день и контролируйте соленую еду, которая может маскировать прогресс.",
            priority = RecommendationPriority.Medium,
        )
    }

    if (latest != null && (latest.skeletalMuscleKg < PlanTargets.InitialSkeletalMuscleKg || (previous != null && latest.skeletalMuscleKg < previous.skeletalMuscleKg))) {
        recommendations += Recommendation(
            title = "Защитите скелетные мышцы",
            description = "Держите белок стабильнее, оставляйте 2-3 повтора в запасе в базовых упражнениях и прогрессируйте один параметр в неделю: повторы, вес или подходы.",
            priority = RecommendationPriority.High,
        )
    }

    if (recommendations.isEmpty()) {
        recommendations += Recommendation(
            title = "Продолжайте по плану",
            description = "Тренд совпадает с 12-недельной целью. Держите привычки стабильными и пересматривайте замеры раз в неделю.",
            priority = RecommendationPriority.Low,
        )
    }

    return recommendations
}

private fun bodyFatStalledForTwoWeeks(measurementsDescending: List<MeasurementEntry>): Boolean {
    val latest = measurementsDescending.firstOrNull() ?: return false
    val older = measurementsDescending.firstOrNull { ChronoUnit.DAYS.between(it.date, latest.date) >= 14 } ?: return false
    return latest.bodyFatPercent >= older.bodyFatPercent
}

fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
