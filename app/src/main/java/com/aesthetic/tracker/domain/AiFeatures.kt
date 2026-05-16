package com.aesthetic.tracker.domain

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.data.WorkoutPlanDay
import java.time.LocalDate

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

data class TodayPlanInput(
    val today: LocalDate,
    val measurementsDescending: List<MeasurementEntry>,
    val currentPlanDay: WorkoutPlanDay?,
    val planPosition: PlanPosition,
    val currentHabit: HabitEntry,
    val selectedFoodGoal: FoodGoal,
)

data class TodayPlanSignature(
    val today: LocalDate,
    val latestMeasurementDate: LocalDate?,
    val latestMeasurementFingerprint: String?,
    val previousMeasurementDate: LocalDate?,
    val previousMeasurementFingerprint: String?,
    val week: Int,
    val day: Int,
    val workoutTitle: String?,
    val workoutFingerprint: String?,
    val habitFingerprint: String,
    val selectedFoodGoal: FoodGoal,
    val targetFingerprint: String,
)

data class GeneratedTodayPlan(
    val signature: TodayPlanSignature,
    val focus: String,
    val training: List<String>,
    val nutrition: List<String>,
    val recovery: List<String>,
    val checkpoints: List<String>,
)

fun TodayPlanInput.signature(): TodayPlanSignature {
    val latest = measurementsDescending.firstOrNull()
    val previous = measurementsDescending.drop(1).firstOrNull()
    return TodayPlanSignature(
        today = today,
        latestMeasurementDate = latest?.date,
        latestMeasurementFingerprint = latest?.fingerprint(),
        previousMeasurementDate = previous?.date,
        previousMeasurementFingerprint = previous?.fingerprint(),
        week = planPosition.week,
        day = planPosition.day,
        workoutTitle = currentPlanDay?.title,
        workoutFingerprint = currentPlanDay?.let { "${it.title}:${it.focus}:${it.exercises.joinToString("|")}" },
        habitFingerprint = listOf(
            currentHabit.waterDone,
            currentHabit.stepsDone,
            currentHabit.proteinDone,
            currentHabit.workoutDone,
            currentHabit.postureDone,
            currentHabit.sleepDone,
        ).joinToString(":"),
        selectedFoodGoal = selectedFoodGoal,
        targetFingerprint = listOf(
            PlanTargets.TargetMinWeightKg,
            PlanTargets.TargetMaxWeightKg,
            PlanTargets.TargetMinBodyFatPercent,
            PlanTargets.TargetMaxBodyFatPercent,
            PlanTargets.TargetMuscleGainMinKg,
            PlanTargets.TargetMuscleGainMaxKg,
            PlanTargets.InitialPulse,
        ).joinToString(":"),
    )
}

private fun MeasurementEntry.fingerprint(): String = listOf(
    weightKg.format(1),
    bodyFatPercent.format(1),
    skeletalMuscleKg.format(1),
    pulse.toString(),
    visceralFat.toString(),
    waterPercent.format(1),
).joinToString(":")

fun buildGeneratedTodayPlan(input: TodayPlanInput): GeneratedTodayPlan {
    val latest = input.measurementsDescending.firstOrNull()
    val previous = input.measurementsDescending.drop(1).firstOrNull()
    val dishes = FoodRecommendations.filter { input.selectedFoodGoal in it.goals }.ifEmpty {
        FoodRecommendations.filter { FoodGoal.HighProtein in it.goals }
    }
    val hasHighPulse = latest?.pulse?.let { it >= 90 } == true
    val waterIsLow = latest?.waterPercent?.let { it < 50.0 } == true
    val muscleDropped = latest != null && previous != null && latest.skeletalMuscleKg < previous.skeletalMuscleKg

    val focus = when {
        latest == null -> "Собрать точку отсчета и выполнить базовый день без перегруза"
        hasHighPulse -> "Снизить нагрузку сегодня и сохранить движение без лишнего стресса"
        muscleDropped -> "Защитить мышцы: белок, техника и умеренная прогрессия"
        else -> input.currentPlanDay?.focus ?: "Выполнить план дня и закрыть ключевые привычки"
    }

    val training = buildList {
        val planDay = input.currentPlanDay
        if (planDay == null) {
            add("Сделайте 35-45 минут ходьбы в комфортном темпе и 10 минут мобилити.")
        } else if (hasHighPulse) {
            add("Оставьте тренировку ${planDay.title} в легком режиме: RPE 6/10, без отказных подходов.")
            add("Выберите 3 главных упражнения: ${planDay.exercises.take(3).joinToString(", ")}.")
            add("Завершите 20-30 минутами ходьбы в зоне 2 вместо интервалов.")
        } else {
            add("Выполните ${planDay.title}: ${planDay.exercises.take(4).joinToString(", ")}.")
            add("В одном базовом упражнении добавьте 1 повтор или небольшой вес, если техника стабильна.")
            add("Оставьте 2 повтора в запасе и не превращайте день в тест максимума.")
        }
        if (latest == null) add("Перед тренировкой добавьте замер или загрузку весов, чтобы следующий план был точнее.")
    }

    val nutrition = buildList {
        val proteinTarget = latest?.let { "${(it.weightKg * 1.8).format(0)}-${(it.weightKg * 2.1).format(0)} г белка" }
            ?: "120-145 г белка"
        add("Цель питания сегодня: $proteinTarget, 2-3 приема с явным белковым блюдом.")
        dishes.take(2).forEach { dish ->
            add("${dish.dish}: ${dish.caloriesEstimate}, ${dish.proteinEstimate}. Избегать: ${dish.avoid}")
        }
        if (waterIsLow) {
            add("Добавьте 500-700 мл воды в первой половине дня и выбирайте менее соленую доставку.")
        } else {
            add("Сладкие напитки, майонезные соусы и жареные гарниры сегодня не помогают цели.")
        }
    }

    val recovery = buildList {
        if (hasHighPulse) add("Поставьте сон выше дополнительного кардио: цель 7,5-9 часов.")
        if (waterIsLow) add("Отследите воду и соль: низкий процент воды может маскировать прогресс.")
        if (muscleDropped) add("После тренировки добавьте белковый прием пищи и не урезайте калории агрессивно.")
        if (isEmpty()) add("10 минут мобилити вечером и спокойная прогулка помогут восстановлению.")
    }

    val checkpoints = buildList {
        if (!input.currentHabit.waterDone) add("Вода")
        if (!input.currentHabit.stepsDone) add("Шаги")
        if (!input.currentHabit.proteinDone) add("Белок")
        if (!input.currentHabit.workoutDone) add("Тренировка")
        if (!input.currentHabit.postureDone) add("Осанка")
        if (!input.currentHabit.sleepDone) add("Сон")
    }.ifEmpty { listOf("Все привычки на сегодня закрыты") }

    return GeneratedTodayPlan(
        signature = input.signature(),
        focus = focus,
        training = training,
        nutrition = nutrition,
        recovery = recovery,
        checkpoints = checkpoints,
    )
}
