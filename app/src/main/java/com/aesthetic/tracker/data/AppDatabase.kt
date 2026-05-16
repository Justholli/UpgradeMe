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
    version = 2,
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

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "aesthetic-tracker.db")
                .addMigrations(Migration1To2)
                .build()
    }
}
