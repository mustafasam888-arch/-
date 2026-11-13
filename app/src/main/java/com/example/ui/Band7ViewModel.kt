package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.LiveBandTelemetry
import com.example.ble.MiBand7BleManager
import com.example.ble.ScannedBleDevice
import com.example.data.Band7Repository
import com.example.data.importer.BandExportParser
import com.example.data.local.Band7Database
import com.example.data.local.DailySummaryEntity
import com.example.data.local.HeartRateSampleEntity
import com.example.data.local.SleepSessionEntity
import com.example.data.local.Spo2SampleEntity
import com.example.data.local.StressSampleEntity
import com.example.data.local.WorkoutEntity
import com.example.data.prefs.BandPreferencesRepository
import com.example.data.prefs.BandUserPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class Band7UiState(
    val selectedDateKey: String = BandExportParser.todayKey(),
    val availableDateKeys: List<String> = emptyList(),
    val dailySummary: DailySummaryEntity? = null,
    val heartRateSamples: List<HeartRateSampleEntity> = emptyList(),
    val spo2Samples: List<Spo2SampleEntity> = emptyList(),
    val stressSamples: List<StressSampleEntity> = emptyList(),
    val sleepSession: SleepSessionEntity? = null,
    val allSleepSessions: List<SleepSessionEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val liveTelemetry: LiveBandTelemetry = LiveBandTelemetry(),
    val scannedDevices: List<ScannedBleDevice> = emptyList(),
    val userPrefs: BandUserPrefs = BandUserPrefs(),
    val feedbackBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class Band7ViewModel(application: Application) : AndroidViewModel(application) {

    private val database = Band7Database.getInstance(application)
    private val prefsRepo = BandPreferencesRepository(application)
    private val bleManager = MiBand7BleManager(application)
    private val exportParser = BandExportParser(database.band7Dao())
    private val repository = Band7Repository(
        dao = database.band7Dao(),
        prefsRepo = prefsRepo,
        bleManager = bleManager,
        exportParser = exportParser
    )

    private val selectedDateKeyFlow = MutableStateFlow(BandExportParser.todayKey())
    private val feedbackBannerFlow = MutableStateFlow<String?>(null)

    private val dateScopedFlows = selectedDateKeyFlow.flatMapLatest { dateKey ->
        combine(
            repository.observeSummaryForDate(dateKey),
            repository.observeHeartRateForDate(dateKey),
            repository.observeSpo2ForDate(dateKey),
            repository.observeStressForDate(dateKey),
            repository.observeSleepForDate(dateKey)
        ) { summary, hr, spo2, stress, sleep ->
            DateScopedTelemetry(dateKey, summary, hr, spo2, stress, sleep)
        }
    }

    val uiState: StateFlow<Band7UiState> = combine(
        dateScopedFlows,
        repository.allDailySummaries,
        repository.allSleepSessions,
        repository.allWorkouts,
        bleManager.telemetry,
        bleManager.scannedDevices,
        repository.userPrefs,
        feedbackBannerFlow
    ) { values ->
        val scoped = values[0] as DateScopedTelemetry
        @Suppress("UNCHECKED_CAST")
        val allSummaries = values[1] as List<DailySummaryEntity>
        @Suppress("UNCHECKED_CAST")
        val allSleep = values[2] as List<SleepSessionEntity>
        @Suppress("UNCHECKED_CAST")
        val workouts = values[3] as List<WorkoutEntity>
        val live = values[4] as LiveBandTelemetry
        @Suppress("UNCHECKED_CAST")
        val scanned = values[5] as List<ScannedBleDevice>
        val prefs = values[6] as BandUserPrefs
        val banner = values[7] as String?

        val dates = (allSummaries.map { it.dateKey } + BandExportParser.todayKey())
            .distinct()
            .sortedDescending()

        Band7UiState(
            selectedDateKey = scoped.dateKey,
            availableDateKeys = dates,
            dailySummary = scoped.summary,
            heartRateSamples = scoped.hr,
            spo2Samples = scoped.spo2,
            stressSamples = scoped.stress,
            sleepSession = scoped.sleep,
            allSleepSessions = allSleep,
            workouts = workouts,
            liveTelemetry = live,
            scannedDevices = scanned,
            userPrefs = prefs,
            feedbackBanner = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Band7UiState()
    )

    private data class DateScopedTelemetry(
        val dateKey: String,
        val summary: DailySummaryEntity?,
        val hr: List<HeartRateSampleEntity>,
        val spo2: List<Spo2SampleEntity>,
        val stress: List<StressSampleEntity>,
        val sleep: SleepSessionEntity?
    )

    fun requiredBlePermissions(): Array<String> = bleManager.requiredPermissions()
    fun hasBlePermissions(): Boolean = bleManager.hasRequiredPermissions()
    fun refreshBluetoothState() = bleManager.refreshBluetoothState()

    fun selectDate(dateKey: String) {
        selectedDateKeyFlow.value = dateKey
    }

    fun startBleScan() = bleManager.startScan()
    fun stopBleScan() = bleManager.stopScan()

    fun connectToDevice(device: ScannedBleDevice) {
        viewModelScope.launch {
            repository.savePairedDevice(device.address, device.name)
            val authKey = uiState.value.userPrefs.authKeyHex
            bleManager.connect(device.address, authKey)
        }
    }

    fun reconnectPairedDevice() {
        val prefs = uiState.value.userPrefs
        if (prefs.pairedMacAddress.isNotBlank()) {
            bleManager.connect(prefs.pairedMacAddress, prefs.authKeyHex)
        } else {
            bleManager.startScan()
        }
    }

    fun disconnectDevice() = bleManager.disconnect()
    fun syncNow() = bleManager.syncNow()
    fun findBand(vibrate: Boolean = true) = bleManager.triggerFindBand(vibrate)

    fun readGattCharacteristic(serviceUuid: UUID, charUuid: UUID) {
        bleManager.readCharacteristic(serviceUuid, charUuid)
    }

    fun saveAuthKey(hex: String) {
        viewModelScope.launch {
            repository.saveAuthKeyHex(hex)
            feedbackBannerFlow.value = "تم حفظ مفتاح المصادقة (Auth Key)"
        }
    }

    fun saveStepGoal(goal: Int) {
        viewModelScope.launch {
            repository.saveStepGoal(goal)
            feedbackBannerFlow.value = "تم تحديث هدف الخطوات اليومي إلى $goal خطوة"
        }
    }

    fun toggleTheme(dark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(dark)
        }
    }

    fun importExportFile(uri: Uri) {
        viewModelScope.launch {
            val res = repository.importExportFile(getApplication(), uri)
            res.onSuccess { imported ->
                feedbackBannerFlow.value =
                    "تم استيراد ${imported.importedRecords} سجل (${imported.detectedFormat})"
                val latestDate = uiState.value.availableDateKeys.firstOrNull()
                if (latestDate != null) {
                    selectedDateKeyFlow.value = latestDate
                }
            }.onFailure { err ->
                feedbackBannerFlow.value = err.localizedMessage ?: "فشل استيراد الملف"
            }
        }
    }

    fun clearLocalHistory() {
        viewModelScope.launch {
            repository.clearAllLocalData()
            selectedDateKeyFlow.value = BandExportParser.todayKey()
            feedbackBannerFlow.value = "تم مسح السجل المحلي بنجاح"
        }
    }

    fun dismissBanner() {
        feedbackBannerFlow.update { null }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
        bleManager.disconnect()
    }
}
