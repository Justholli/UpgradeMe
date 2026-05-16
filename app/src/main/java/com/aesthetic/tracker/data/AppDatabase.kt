package com.aesthetic.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MeasurementEntry::class,
        HabitEntry::class,
        WorkoutPlanDay::class,
        ScaleScreenshotImport::class,
        ImportedScheduleDay::class,
        ImportedScheduleEvent::class,
        ScheduleEventCompletion::class,
        ScheduleEventStart::class,
        ImportedMealRecommendation::class,
        ImportedGoal::class,
        ImportedWorkoutExercise::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aestheticDao(): AestheticDao

    companion object {
        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN bmi REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN muscleMassKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN proteinPercent REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN basalMetabolismKcal INTEGER")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN biologicalAge INTEGER")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN scalePhotoPath TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scale_screenshot_imports (
                        id TEXT NOT NULL PRIMARY KEY,
                        createdAtEpochMillis INTEGER NOT NULL,
                        localPath TEXT NOT NULL,
                        parsedWeightKg REAL,
                        parsedBodyFatPercent REAL,
                        parsedBmi REAL,
                        parsedMuscleMassKg REAL,
                        parsedSkeletalMuscleKg REAL,
                        parsedWaterPercent REAL,
                        parsedProteinPercent REAL,
                        parsedVisceralFat INTEGER,
                        parsedBasalMetabolismKcal INTEGER,
                        parsedBiologicalAge INTEGER,
                        parsedPulse INTEGER,
                        isParsed INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS imported_schedule_days (
                        date TEXT NOT NULL PRIMARY KEY,
                        weekStartDate TEXT,
                        focus TEXT,
                        deliveryAddress TEXT,
                        foodProviderName TEXT,
                        foodProviderCity TEXT,
                        foodProviderCityUrl TEXT,
                        foodProviderNote TEXT,
                        nutrition TEXT NOT NULL,
                        recovery TEXT NOT NULL,
                        checkpoints TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS imported_schedule_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        kind TEXT NOT NULL,
                        isFixed INTEGER NOT NULL,
                        notificationText TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS imported_meal_recommendations (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        restaurant TEXT,
                        sourceDescription TEXT,
                        description TEXT,
                        estimatedCalories INTEGER,
                        estimatedProteinG INTEGER,
                        estimatedFatG INTEGER,
                        estimatedCarbsG INTEGER,
                        weightG INTEGER,
                        priceRub INTEGER,
                        foodUrl TEXT,
                        source TEXT,
                        fallback TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS imported_goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        visualReference TEXT,
                        targetWeightKg REAL,
                        targetBodyFatPercentRange TEXT,
                        trainingPrinciples TEXT NOT NULL,
                        focusMuscles TEXT NOT NULL,
                        nutritionPrinciples TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS imported_workout_exercises (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        eventId TEXT,
                        workoutId TEXT,
                        workoutTitle TEXT,
                        workoutDescription TEXT,
                        workoutEstimatedDurationMin INTEGER,
                        workoutIntensity TEXT,
                        exerciseId TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        sets INTEGER,
                        reps TEXT,
                        durationSec INTEGER,
                        restSec INTEGER,
                        rpe TEXT,
                        equipment TEXT,
                        target TEXT NOT NULL,
                        previewImageUrl TEXT,
                        imageUrls TEXT NOT NULL,
                        imageAlt TEXT,
                        sourceUrl TEXT,
                        techniqueSteps TEXT NOT NULL,
                        commonMistakes TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN checkInDone INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_event_completions (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_event_starts (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "aesthetic-tracker.db")
                .addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6, Migration6To7)
                .build()
    }
}
