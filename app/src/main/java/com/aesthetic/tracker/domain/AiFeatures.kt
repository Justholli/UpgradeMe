package com.aesthetic.tracker.domain

import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.data.WorkoutPlanDay
import java.time.LocalDate
import java.time.LocalTime

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
    val schedule: List<TodayScheduleItem>,
    val training: List<TodayTrainingItem>,
    val nutrition: List<String>,
    val recovery: List<String>,
    val checkpoints: List<String>,
    val prompt: String,
)

data class TodayTrainingItem(
    val title: String,
    val description: String,
)

data class TodayScheduleItem(
    val id: String,
    val time: LocalTime,
    val title: String,
    val description: String,
    val kind: TodayScheduleKind,
    val isFixed: Boolean,
    val notificationText: String,
)

enum class TodayScheduleKind { Sleep, Meal, Workout, Recovery, CheckIn, Habit }

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

fun buildTodayPlanPrompt(input: TodayPlanInput): String {
    val latest = input.measurementsDescending.firstOrNull()
    val previous = input.measurementsDescending.drop(1).firstOrNull()
    val latestBlock = latest?.let {
        "latest=${it.date}: weight=${it.weightKg.format(1)}kg, fat=${it.bodyFatPercent.format(1)}%, muscle=${it.skeletalMuscleKg.format(1)}kg, pulse=${it.pulse}, water=${it.waterPercent.format(1)}%"
    } ?: "latest=missing"
    val previousBlock = previous?.let {
        "previous=${it.date}: weight=${it.weightKg.format(1)}kg, fat=${it.bodyFatPercent.format(1)}%, muscle=${it.skeletalMuscleKg.format(1)}kg, pulse=${it.pulse}, water=${it.waterPercent.format(1)}%"
    } ?: "previous=missing"
    val workoutBlock = input.currentPlanDay?.let {
        "workout=week ${it.week}, day ${it.day}, title=${it.title}, focus=${it.focus}, exercises=${it.exercises.joinToString("; ")}"
    } ?: "workout=missing"

    return """
        Generate a 7-day recomposition schedule from the provided plan and body data as JSON.
        Start with today=${input.today}; include 7 consecutive calendar dates in days[].
        If a scale screenshot is provided, also return optional measurement data in measurement{} using numeric values where visible.
        Keep these anchors unchanged every day: sleep 02:00-10:00, breakfast 12:00, lunch 15:00, dinner 20:00.
        Choose workout timing for each day based on plan day, pulse, recovery risk, and meal anchors.
        Write training[] as objects with title and description. title is the exercise/block name; description includes sets x reps or duration, rest, RPE/intensity, and one technique note.
        Use concise Russian output with timeline items, notification text, and execution checkpoints.
        Schedule kind values must be one of: Sleep, Meal, Workout, Recovery, CheckIn, Habit.
        Data: today=${input.today}, week=${input.planPosition.week}, day=${input.planPosition.day}, foodGoal=${input.selectedFoodGoal}.
        Habit status: water=${input.currentHabit.waterDone}, steps=${input.currentHabit.stepsDone}, protein=${input.currentHabit.proteinDone}, workout=${input.currentHabit.workoutDone}, posture=${input.currentHabit.postureDone}, sleep=${input.currentHabit.sleepDone}.
        Targets: weight=${PlanTargets.TargetMinWeightKg}-${PlanTargets.TargetMaxWeightKg}kg, fat=${PlanTargets.TargetMinBodyFatPercent}-${PlanTargets.TargetMaxBodyFatPercent}%, muscleGain=${PlanTargets.TargetMuscleGainMinKg}-${PlanTargets.TargetMuscleGainMaxKg}kg.
        Measurement keys: date, bodyScore, weightKg, bodyFatPercent, fatMassKg, skeletalMuscleKg, muscleMassKg, muscleRatePercent, pulse, visceralFat, waterPercent, bodyWaterKg, bmi, mineralMassKg, proteinMassKg, proteinPercent, subcutaneousFatPercent, leanBodyMassKg, basalMetabolismKcal, biologicalAge, bodyType, standardWeightKg, weightControlKg, fatControlKg, muscleControlKg.
        $latestBlock
        $previousBlock
        $workoutBlock
    """.trimIndent()
}

private fun MeasurementEntry.fingerprint(): String = listOf(
    bodyScore?.toString().orEmpty(),
    weightKg.format(1),
    bodyFatPercent.format(1),
    fatMassKg?.format(1).orEmpty(),
    skeletalMuscleKg.format(1),
    muscleMassKg?.format(1).orEmpty(),
    muscleRatePercent?.format(1).orEmpty(),
    pulse.toString(),
    visceralFat.toString(),
    waterPercent.format(1),
    bodyWaterKg?.format(1).orEmpty(),
    bmi?.format(1).orEmpty(),
    mineralMassKg?.format(1).orEmpty(),
    proteinMassKg?.format(1).orEmpty(),
    proteinPercent?.format(1).orEmpty(),
    subcutaneousFatPercent?.format(1).orEmpty(),
    leanBodyMassKg?.format(1).orEmpty(),
    basalMetabolismKcal?.toString().orEmpty(),
    biologicalAge?.toString().orEmpty(),
    bodyType.orEmpty(),
    standardWeightKg?.format(1).orEmpty(),
    weightControlKg?.format(1).orEmpty(),
    fatControlKg?.format(1).orEmpty(),
    muscleControlKg?.format(1).orEmpty(),
).joinToString(":")
