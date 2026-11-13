package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailySummaryEntity::class,
        HeartRateSampleEntity::class,
        Spo2SampleEntity::class,
        StressSampleEntity::class,
        SleepSessionEntity::class,
        WorkoutEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class Band7Database : RoomDatabase() {
    abstract fun band7Dao(): Band7Dao

    companion object {
        @Volatile
        private var instance: Band7Database? = null

        fun getInstance(context: Context): Band7Database {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    Band7Database::class.java,
                    "band7_telemetry.db"
                ).build().also { instance = it }
            }
        }
    }
}
