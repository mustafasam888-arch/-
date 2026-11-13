package com.example.data

import android.content.Context
import android.net.Uri
import com.example.ble.BandTelemetryEvent
import com.example.ble.MiBand7BleManager
import com.example.data.importer.BandExportParser
import com.example.data.importer.ImportResult
import com.example.data.local.Band7Dao
import com.example.data.local.DailySummaryEntity
import com.example.data.local.HeartRateSampleEntity
import com.example.data.local.SleepSessionEntity
import com.example.data.local.Spo2SampleEntity
import com.example.data.local.StressSampleEntity
import com.example.data.local.WorkoutEntity
import com.example.data.prefs.BandPreferencesRepository
import com.example.data.prefs.BandUserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Band7Repository(
    private val dao: Band7Dao,
    private val prefsRepo: BandPreferencesRepository,
    val bleManager: MiBand7BleManager,
    private val exportParser: BandExportParser
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userPrefs: Flow<BandUserPrefs> = prefsRepo.prefsFlow
    val allDailySummaries: Flow<List<DailySummaryEntity>> = dao.observeAllDailySummaries()
    val allSleepSessions: Flow<List<SleepSessionEntity>> = dao.observeAllSleepSessions()
    val allWorkouts: Flow<List<WorkoutEntity>> = dao.observeAllWorkouts()

    init {
        observeBleTelemetryEvents()
    }

    fun observeSummaryForDate(dateKey: String): Flow<DailySummaryEntity?> = dao.observeDailySummary(dateKey)
    fun observeHeartRateForDate(dateKey: String): Flow<List<HeartRateSampleEntity>> = dao.observeHeartRateForDate(dateKey)
    fun observeSpo2ForDate(dateKey: String): Flow<List<Spo2SampleEntity>> = dao.observeSpo2ForDate(dateKey)
    fun observeStressForDate(dateKey: String): Flow<List<StressSampleEntity>> = dao.observeStressForDate(dateKey)
    fun observeSleepForDate(dateKey: String): Flow<SleepSessionEntity?> = dao.observeSleepForDate(dateKey)

    private fun observeBleTelemetryEvents() {
        scope.launch {
            bleManager.events.collect { event ->
                val today = BandExportParser.todayKey()
                val prefs = prefsRepo.prefsFlow.first()
                val current = dao.getDailySummary(today) ?: DailySummaryEntity(
                    dateKey = today,
                    stepGoal = prefs.stepGoal
                )

                when (event) {
                    is BandTelemetryEvent.HeartRateReceived -> {
                        dao.insertHeartRateSample(
                            HeartRateSampleEntity(
                                dateKey = today,
                                timestampMillis = event.timestampMillis,
                                bpm = event.bpm
                            )
                        )
                        val minHr = if (current.minHr == 0) event.bpm else minOf(current.minHr, event.bpm)
                        val maxHr = maxOf(current.maxHr, event.bpm)
                        val avgHr = if (current.avgHr == 0) event.bpm else (current.avgHr + event.bpm) / 2
                        dao.upsertDailySummary(
                            current.copy(
                                minHr = minHr,
                                maxHr = maxHr,
                                avgHr = avgHr,
                                syncedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }

                    is BandTelemetryEvent.StepsReceived -> {
                        dao.upsertDailySummary(
                            current.copy(
                                steps = event.steps,
                                distanceMeters = event.distanceMeters,
                                activeCalories = event.calories,
                                totalCalories = maxOf(current.totalCalories, event.calories),
                                syncedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }

                    is BandTelemetryEvent.BatteryReceived -> {
                        dao.upsertDailySummary(
                            current.copy(
                                batteryLevel = event.level,
                                isCharging = event.isCharging,
                                lastChargedInfo = event.lastChargedDate.ifBlank { current.lastChargedInfo },
                                syncedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }

                    is BandTelemetryEvent.DeviceInfoUpdated -> {
                        dao.upsertDailySummary(
                            current.copy(
                                deviceName = event.deviceName.ifBlank { current.deviceName },
                                macAddress = event.macAddress.ifBlank { current.macAddress },
                                firmwareVersion = event.firmware.ifBlank { current.firmwareVersion },
                                hardwareVersion = event.hardware.ifBlank { current.hardwareVersion },
                                serialNumber = event.serial.ifBlank { current.serialNumber },
                                syncedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun savePairedDevice(macAddress: String, name: String) {
        prefsRepo.savePairedDevice(macAddress, name)
    }

    suspend fun saveAuthKeyHex(hex: String) {
        prefsRepo.saveAuthKeyHex(hex)
    }

    suspend fun saveStepGoal(goal: Int) {
        prefsRepo.saveStepGoal(goal)
        val today = BandExportParser.todayKey()
        dao.getDailySummary(today)?.let { existing ->
            dao.upsertDailySummary(existing.copy(stepGoal = goal))
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        prefsRepo.setDarkTheme(enabled)
    }

    suspend fun importExportFile(context: Context, uri: Uri): Result<ImportResult> {
        val prefs = prefsRepo.prefsFlow.first()
        return exportParser.importFromUri(context, uri, prefs.stepGoal)
    }

    suspend fun clearAllLocalData() {
        dao.clearSummaries()
        dao.clearHeartRates()
        dao.clearSpo2()
        dao.clearStress()
        dao.clearSleep()
        dao.clearWorkouts()
    }
}
