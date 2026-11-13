package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface Band7Dao {

    @Query("SELECT * FROM daily_summaries ORDER BY dateKey DESC")
    fun observeAllDailySummaries(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey LIMIT 1")
    fun observeDailySummary(dateKey: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailySummary(dateKey: String): DailySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM heart_rate_samples WHERE dateKey = :dateKey ORDER BY timestampMillis ASC")
    fun observeHeartRateForDate(dateKey: String): Flow<List<HeartRateSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRateSamples(samples: List<HeartRateSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRateSample(sample: HeartRateSampleEntity)

    @Query("SELECT * FROM spo2_samples WHERE dateKey = :dateKey ORDER BY timestampMillis ASC")
    fun observeSpo2ForDate(dateKey: String): Flow<List<Spo2SampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpo2Samples(samples: List<Spo2SampleEntity>)

    @Query("SELECT * FROM stress_samples WHERE dateKey = :dateKey ORDER BY timestampMillis ASC")
    fun observeStressForDate(dateKey: String): Flow<List<StressSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStressSamples(samples: List<StressSampleEntity>)

    @Query("SELECT * FROM sleep_sessions ORDER BY endMillis DESC")
    fun observeAllSleepSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE dateKey = :dateKey ORDER BY endMillis DESC LIMIT 1")
    fun observeSleepForDate(dateKey: String): Flow<SleepSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSessions(sessions: List<SleepSessionEntity>)

    @Query("SELECT * FROM workouts ORDER BY startMillis DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Query("DELETE FROM daily_summaries")
    suspend fun clearSummaries()

    @Query("DELETE FROM heart_rate_samples")
    suspend fun clearHeartRates()

    @Query("DELETE FROM spo2_samples")
    suspend fun clearSpo2()

    @Query("DELETE FROM stress_samples")
    suspend fun clearStress()

    @Query("DELETE FROM sleep_sessions")
    suspend fun clearSleep()

    @Query("DELETE FROM workouts")
    suspend fun clearWorkouts()
}
