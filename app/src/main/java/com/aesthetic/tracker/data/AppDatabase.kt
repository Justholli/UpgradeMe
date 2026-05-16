package com.aesthetic.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MeasurementEntry::class, HabitEntry::class, WorkoutPlanDay::class, ScaleScreenshotImport::class],
    version = 3,
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
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN bodyScore INTEGER")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN fatMassKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN muscleRatePercent REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN bodyWaterKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN mineralMassKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN proteinMassKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN subcutaneousFatPercent REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN leanBodyMassKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN bodyType TEXT")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN standardWeightKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN weightControlKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN fatControlKg REAL")
                db.execSQL("ALTER TABLE measurement_entries ADD COLUMN muscleControlKg REAL")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "aesthetic-tracker.db")
                .addMigrations(Migration1To2, Migration2To3)
                .build()
    }
}
