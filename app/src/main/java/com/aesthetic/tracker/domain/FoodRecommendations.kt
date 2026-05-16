package com.aesthetic.tracker.domain

import androidx.annotation.StringRes
import com.aesthetic.tracker.R

enum class FoodGoal(@StringRes val labelRes: Int) {
    Breakfast(R.string.food_goal_breakfast),
    Lunch(R.string.food_goal_lunch),
    Dinner(R.string.food_goal_dinner),
    HighProtein(R.string.food_goal_high_protein),
    LowSalt(R.string.food_goal_low_salt),
    PostWorkout(R.string.food_goal_post_workout),
}

data class FoodRecommendation(
    val dish: String,
    val caloriesEstimate: String,
    val proteinEstimate: String,
    val whyItFits: String,
    val avoid: String,
    val yandexQuery: String,
    val goals: Set<FoodGoal>,
)

val FoodRecommendations = listOf(
    FoodRecommendation(
        dish = "Поке с курицей или лососем",
        caloriesEstimate = "520-750 kcal",
        proteinEstimate = "32-45 г белка",
        whyItFits = "Баланс белка, риса и овощей, а соус легко контролировать под рекомпозицию.",
        avoid = "Майонез, темпура, сладкие напитки и лишние соусы.",
        yandexQuery = "поке лосось",
        goals = setOf(FoodGoal.Lunch, FoodGoal.Dinner, FoodGoal.HighProtein, FoodGoal.PostWorkout),
    ),
    FoodRecommendation(
        dish = "Курица гриль с рисом и овощами",
        caloriesEstimate = "480-680 kcal",
        proteinEstimate = "38-55 г белка",
        whyItFits = "Простая высокобелковая тарелка с углеводами для тренировок и овощами для сытости.",
        avoid = "Жареные гарниры, сливочные соусы, сладкие напитки и лишнее масло.",
        yandexQuery = "гриль курица рис овощи",
        goals = setOf(FoodGoal.Lunch, FoodGoal.Dinner, FoodGoal.HighProtein, FoodGoal.LowSalt, FoodGoal.PostWorkout),
    ),
    FoodRecommendation(
        dish = "Салат с индейкой или курицей",
        caloriesEstimate = "350-550 kcal",
        proteinEstimate = "28-42 г белка",
        whyItFits = "Постный белок и большой объем еды, когда нужен более легкий заказ.",
        avoid = "Майонезные заправки, сухарики, жареную курицу и лишние соусы.",
        yandexQuery = "салат индейка",
        goals = setOf(FoodGoal.Lunch, FoodGoal.Dinner, FoodGoal.HighProtein, FoodGoal.LowSalt),
    ),
    FoodRecommendation(
        dish = "Омлет или яйца с творогом",
        caloriesEstimate = "380-600 kcal",
        proteinEstimate = "30-45 г белка",
        whyItFits = "Удобный завтрак с белком и медленно усваиваемым творогом для сытости.",
        avoid = "Майонез, жареный картофель, сладкие напитки и тяжелые сырные соусы.",
        yandexQuery = "омлет творог",
        goals = setOf(FoodGoal.Breakfast, FoodGoal.HighProtein),
    ),
    FoodRecommendation(
        dish = "Греческий йогурт с ягодами",
        caloriesEstimate = "250-420 kcal",
        proteinEstimate = "18-30 г белка",
        whyItFits = "Легкий завтрак или перекус с белком без лишней соли.",
        avoid = "Сладкие сиропы, избыток гранолы, выпечку и сладкие напитки.",
        yandexQuery = "греческий йогурт ягоды",
        goals = setOf(FoodGoal.Breakfast, FoodGoal.LowSalt),
    ),
    FoodRecommendation(
        dish = "Шаурма с курицей или говядиной без майонеза",
        caloriesEstimate = "500-750 kcal",
        proteinEstimate = "30-45 г белка",
        whyItFits = "Есть почти везде и подходит, если контролировать соус и жареные добавки.",
        avoid = "Майонез, картофель фри, сладкие напитки и лишние соусы.",
        yandexQuery = "шаурма курица без майонеза",
        goals = setOf(FoodGoal.Lunch, FoodGoal.Dinner, FoodGoal.HighProtein),
    ),
    FoodRecommendation(
        dish = "Том ям с морепродуктами",
        caloriesEstimate = "300-520 kcal",
        proteinEstimate = "22-35 г белка",
        whyItFits = "Белок из морепродуктов и объем супа дают сытость без большого избытка калорий.",
        avoid = "Лишние сливки, жареные гарниры, сладкие напитки и соленые добавки.",
        yandexQuery = "том ям морепродукты",
        goals = setOf(FoodGoal.Lunch, FoodGoal.Dinner, FoodGoal.LowSalt),
    ),
    FoodRecommendation(
        dish = "Суп и основное блюдо с белком",
        caloriesEstimate = "550-800 kcal",
        proteinEstimate = "35-55 г белка",
        whyItFits = "Суп добавляет сытость, а белковое основное блюдо держит рацион в плане.",
        avoid = "Крем-супы, жареные гарниры, майонез, сладкие напитки и лишние соусы.",
        yandexQuery = "суп курица рис овощи",
        goals = setOf(FoodGoal.Lunch, FoodGoal.Dinner, FoodGoal.HighProtein),
    ),
)
