package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val dateKey: String,
    val steps: Int = 0,
    val stepGoal: Int = 8000,
    val distanceMeters: Int = 0,
    val activeCalories: Int = 0,
    val totalCalories: Int = 0,
    val activeMinutes: Int = 0,
    val standingHours: Int = 0,
    val paiScore: Int = 0,
    val paiToday: Int = 0,
    val restingHr: Int = 0,
    val avgHr: Int = 0,
    val minHr: Int = 0,
    val maxHr: Int = 0,
    val latestSpo2: Int = 0,
    val minSpo2: Int = 0,
    val maxSpo2: Int = 0,
    val avgStress: Int = 0,
    val minStress: Int = 0,
    val maxStress: Int = 0,
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val lastChargedInfo: String = "",
    val deviceName: String = "",
    val macAddress: String = "",
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val serialNumber: String = "",
    val syncedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "heart_rate_samples")
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val timestampMillis: Long,
    val bpm: Int
)

@Entity(tableName = "spo2_samples")
data class Spo2SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val timestampMillis: Long,
    val percentage: Int
)

@Entity(tableName = "stress_samples")
data class StressSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val timestampMillis: Long,
    val level: Int
)

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val startMillis: Long,
    val endMillis: Long,
    val totalMinutes: Int,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val remMinutes: Int,
    val awakeMinutes: Int,
    val napMinutes: Int = 0,
    val sleepScore: Int = 0,
    val breathingScore: Int = 0,
    /** Encoded segments like "L:40,D:35,L:20,R:25,A:5" */
    val stagesEncoded: String = ""
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val workoutType: String,
    val titleAr: String,
    val startMillis: Long,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val calories: Int,
    val avgHr: Int,
    val maxHr: Int,
    val avgPaceSecPerKm: Int = 0,
    val aerobicTe: Float = 0f,
    val anaerobicTe: Float = 0f,
    val vo2Max: Int = 0,
    val steps: Int = 0
)
