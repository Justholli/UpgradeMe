package com.aesthetic.tracker.data

object DefaultWorkoutPlan {
    val days: List<WorkoutPlanDay> = (1..12).flatMap { week ->
        val phase = when (week) {
            in 1..4 -> "Foundation"
            in 5..8 -> "Progressive overload"
            else -> "Refinement"
        }
        listOf(
            WorkoutPlanDay(week, 1, "$phase Push", listOf("Incline dumbbell press", "Shoulder press", "Cable fly", "Lateral raise", "Triceps pressdown"), "Chest, shoulders, triceps"),
            WorkoutPlanDay(week, 2, "$phase Pull", listOf("Lat pulldown", "Seated row", "Rear delt fly", "Dumbbell curl", "Face pull"), "Back, rear delts, biceps"),
            WorkoutPlanDay(week, 3, "Zone 2 + Mobility", listOf("35 min brisk walk", "Hip flexor stretch", "Thoracic rotations", "Deep nasal breathing"), "Recovery and pulse control"),
            WorkoutPlanDay(week, 4, "$phase Legs", listOf("Goblet squat", "Romanian deadlift", "Leg press", "Hamstring curl", "Calf raise"), "Legs and posterior chain"),
            WorkoutPlanDay(week, 5, "Upper Aesthetic", listOf("Pull-ups or assisted pull-ups", "Incline press", "Cable row", "Lateral raise mechanical drop set", "Core plank"), "V-taper and posture"),
            WorkoutPlanDay(week, 6, "Conditioning", listOf("8,000-10,000 steps", "20 min easy bike", "Posture reset", "Light band work"), "Cardio base and movement quality"),
            WorkoutPlanDay(week, 7, "Rest + Review", listOf("Morning weigh-in", "Progress photos", "Meal prep", "Sleep routine"), "Recovery and consistency"),
        )
    }
}
