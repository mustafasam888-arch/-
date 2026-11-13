package com.example.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

enum class BleConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    DISCOVERING_SERVICES,
    AUTHENTICATING,
    CONNECTED
}

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isXiaomiBandCandidate: Boolean,
    val lastSeenMillis: Long = System.currentTimeMillis()
)

data class DiscoveredGattCharacteristic(
    val uuid: UUID,
    val titleAr: String,
    val propertiesLabel: String,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canNotify: Boolean,
    val lastRawHex: String = "",
    val decodedValue: String = ""
)

data class DiscoveredGattService(
    val uuid: UUID,
    val titleAr: String,
    val characteristics: List<DiscoveredGattCharacteristic>
)

data class LiveBandTelemetry(
    val connectionStatus: BleConnectionStatus = BleConnectionStatus.DISCONNECTED,
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val connectedName: String = "",
    val connectedMac: String = "",
    val rssi: Int = 0,
    val authStatusMessage: String = "",
    val isAuthenticated: Boolean = false,
    val manufacturer: String = "",
    val modelNumber: String = "",
    val serialNumber: String = "",
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val softwareVersion: String = "",
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val lastChargedLevel: Int = 0,
    val lastChargedDate: String = "",
    val liveHeartRateBpm: Int = 0,
    val liveSteps: Int = 0,
    val liveDistanceMeters: Int = 0,
    val liveCalories: Int = 0,
    val statusLog: List<String> = emptyList(),
    val discoveredServices: List<DiscoveredGattService> = emptyList(),
    val errorMessage: String? = null
)

sealed interface BandTelemetryEvent {
    data class HeartRateReceived(val bpm: Int, val timestampMillis: Long) : BandTelemetryEvent
    data class StepsReceived(val steps: Int, val distanceMeters: Int, val calories: Int) : BandTelemetryEvent
    data class BatteryReceived(
        val level: Int,
        val isCharging: Boolean,
        val lastChargedDate: String
    ) : BandTelemetryEvent
    data class DeviceInfoUpdated(
        val deviceName: String,
        val macAddress: String,
        val firmware: String,
        val hardware: String,
        val serial: String
    ) : BandTelemetryEvent
}

class MiBand7BleManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bluetoothGatt: BluetoothGatt? = null
    private var currentAuthKeyBytes: ByteArray? = null

    private val gattOpQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private var isGattOpInFlight: Boolean = false

    private val _scannedDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedBleDevice>> = _scannedDevices.asStateFlow()

    private val _telemetry = MutableStateFlow(refreshAdapterSnapshot(LiveBandTelemetry()))
    val telemetry: StateFlow<LiveBandTelemetry> = _telemetry.asStateFlow()

    private val _events = MutableSharedFlow<BandTelemetryEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<BandTelemetryEvent> = _events.asSharedFlow()

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshBluetoothState() {
        _telemetry.update { refreshAdapterSnapshot(it) }
    }

    private fun refreshAdapterSnapshot(current: LiveBandTelemetry): LiveBandTelemetry {
        val adapter = bluetoothAdapter
        return current.copy(
            isBluetoothSupported = adapter != null,
            isBluetoothEnabled = adapter?.isEnabled == true
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshBluetoothState()
        val adapter = bluetoothAdapter
        if (adapter == null) {
            setError("محول البلوتوث (BLE) غير متوفر على هذا الجهاز")
            return
        }
        if (!adapter.isEnabled) {
            setError("يرجى تشغيل البلوتوث أولاً للبحث عن سوار شاومي باند 7")
            return
        }
        if (!hasRequiredPermissions()) {
            setError("يرجى منح أذونات البلوتوث المطلوبة أولاً")
            return
        }

        val leScanner = scanner
        if (leScanner == null) {
            setError("ماسح BLE غير متاح حالياً")
            return
        }

        _scannedDevices.value = emptyList()
        _telemetry.update {
            it.copy(
                isScanning = true,
                connectionStatus = if (it.connectionStatus == BleConnectionStatus.CONNECTED) {
                    BleConnectionStatus.CONNECTED
                } else {
                    BleConnectionStatus.SCANNING
                },
                errorMessage = null
            )
        }
        appendLog("بدأ البحث عن أجهزة BLE القريبة...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching {
            leScanner.startScan(null, settings, scanCallback)
            mainHandler.removeCallbacks(stopScanRunnable)
            mainHandler.postDelayed(stopScanRunnable, 15_000L)
        }.onFailure { err ->
            setError("تعذر بدء البحث: ${err.localizedMessage ?: "خطأ غير معروف"}")
            _telemetry.update { it.copy(isScanning = false) }
        }
    }

    private val stopScanRunnable = Runnable { stopScan() }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        mainHandler.removeCallbacks(stopScanRunnable)
        if (!hasRequiredPermissions()) return
        runCatching { scanner?.stopScan(scanCallback) }
        _telemetry.update { state ->
            state.copy(
                isScanning = false,
                connectionStatus = if (state.connectionStatus == BleConnectionStatus.SCANNING) {
                    BleConnectionStatus.DISCONNECTED
                } else {
                    state.connectionStatus
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, authKeyHex: String) {
        stopScan()
        refreshBluetoothState()
        val adapter = bluetoothAdapter ?: run {
            setError("محول البلوتوث غير متوفر")
            return
        }
        if (!adapter.isEnabled) {
            setError("البلوتوث مغلق. يرجى تفعيله ثم المحاولة مجدداً")
            return
        }
        if (!hasRequiredPermissions()) {
            setError("أذونات البلوتوث غير ممنوحة")
            return
        }

        currentAuthKeyBytes = MiBand7BleConstants.hexStringToBytes(authKeyHex)?.takeIf { it.size == 16 }

        disconnectInternal(keepError = false)
        runCatching {
            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            val safeName = runCatching { device.name }.getOrNull().orEmpty().ifBlank { "Xiaomi Smart Band 7" }
            _telemetry.update {
                it.copy(
                    connectionStatus = BleConnectionStatus.CONNECTING,
                    connectedName = safeName,
                    connectedMac = address,
                    errorMessage = null,
                    isAuthenticated = false,
                    authStatusMessage = ""
                )
            }
            appendLog("جارٍ الاتصال بـ $safeName ($address)...")
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }.onFailure { err ->
            setError("فشل بدء الاتصال بالسوار: ${err.localizedMessage ?: "عنوان غير صالح"}")
        }
    }

    fun disconnect() {
        disconnectInternal(keepError = false)
        appendLog("تم قطع الاتصال بالسوار")
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal(keepError: Boolean) {
        synchronized(gattOpQueue) {
            gattOpQueue.clear()
            isGattOpInFlight = false
        }
        if (hasRequiredPermissions()) {
            runCatching {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
        } else {
            runCatching { bluetoothGatt?.close() }
        }
        bluetoothGatt = null
        _telemetry.update {
            it.copy(
                connectionStatus = BleConnectionStatus.DISCONNECTED,
                isAuthenticated = false,
                errorMessage = if (keepError) it.errorMessage else null
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun triggerFindBand(highAlert: Boolean = true) {
        val gatt = bluetoothGatt ?: run {
            setError("السوار غير متصل حالياً")
            return
        }
        if (!hasRequiredPermissions()) return
        val service = gatt.getService(MiBand7BleConstants.SERVICE_IMMEDIATE_ALERT)
        val char = service?.getCharacteristic(MiBand7BleConstants.CHAR_ALERT_LEVEL)
        if (char == null) {
            setError("خدمة التنبيه الفوري (0x1802) غير متوفرة على هذا الجهاز")
            return
        }
        val payload = byteArrayOf(if (highAlert) 0x02 else 0x00)
        enqueueGattOp {
            writeCharacteristicCompat(gatt, char, payload, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        }
        appendLog(if (highAlert) "تم إرسال أمر اهتزاز للعثور على السوار" else "تم إيقاف التنبيه")
    }

    @SuppressLint("MissingPermission")
    fun syncNow() {
        val gatt = bluetoothGatt ?: run {
            setError("السوار غير متصل لمزامنة البيانات")
            return
        }
        if (!hasRequiredPermissions()) return
        appendLog("بدء مزامنة جميع الخصائص المقروءة...")
        queueInitialReads(gatt)
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(serviceUuid: UUID, charUuid: UUID) {
        val gatt = bluetoothGatt ?: return
        if (!hasRequiredPermissions()) return
        val char = gatt.getService(serviceUuid)?.getCharacteristic(charUuid) ?: return
        enqueueGattOp {
            if (!gatt.readCharacteristic(char)) {
                finishGattOp()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val rawName = runCatching { device.name }.getOrNull()
                ?: result.scanRecord?.deviceName
                ?: ""
            val uuids = result.scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
            val isBandCandidate = rawName.contains("Band", ignoreCase = true) ||
                rawName.contains("Xiaomi", ignoreCase = true) ||
                rawName.contains("Mi", ignoreCase = true) ||
                rawName.contains("Amazfit", ignoreCase = true) ||
                uuids.any {
                    it == MiBand7BleConstants.SERVICE_MIBAND_MAIN ||
                        it == MiBand7BleConstants.SERVICE_MIBAND_AUTH
                }

            val displayName = rawName.ifBlank { "جهاز BLE غير مسمى" }
            _scannedDevices.update { list ->
                val filtered = list.filterNot { it.address == address }
                (filtered + ScannedBleDevice(
                    name = displayName,
                    address = address,
                    rssi = result.rssi,
                    isXiaomiBandCandidate = isBandCandidate
                )).sortedWith(
                    compareByDescending<ScannedBleDevice> { it.isXiaomiBandCandidate }
                        .thenByDescending { it.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            setError("فشل البحث عن البلوتوث (رمز: $errorCode)")
            _telemetry.update { it.copy(isScanning = false, connectionStatus = BleConnectionStatus.DISCONNECTED) }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                _telemetry.update {
                    it.copy(
                        connectionStatus = BleConnectionStatus.DISCOVERING_SERVICES,
                        errorMessage = null
                    )
                }
                appendLog("تم الاتصال! جارٍ اكتشاف خدمات GATT...")
                gatt.discoverServices()
                gatt.readRemoteRssi()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                synchronized(gattOpQueue) {
                    gattOpQueue.clear()
                    isGattOpInFlight = false
                }
                _telemetry.update {
                    it.copy(
                        connectionStatus = BleConnectionStatus.DISCONNECTED,
                        isAuthenticated = false,
                        errorMessage = if (status != BluetoothGatt.GATT_SUCCESS) {
                            "انقطع الاتصال بالسوار (رمز الحالة: $status)"
                        } else {
                            null
                        }
                    )
                }
                appendLog("تم فصل الاتصال (الحالة: $status)")
                runCatching { gatt.close() }
                if (bluetoothGatt == gatt) bluetoothGatt = null
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _telemetry.update { it.copy(rssi = rssi) }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                setError("فشل اكتشاف خدمات السوار ($status)")
                return
            }

            val servicesUi = gatt.services.map { service ->
                val charsUi = service.characteristics.map { ch ->
                    val props = buildList {
                        if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) add("READ")
                        if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) add("WRITE")
                        if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) add("WRITE_NR")
                        if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) add("NOTIFY")
                        if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) add("INDICATE")
                    }.joinToString(" • ")

                    DiscoveredGattCharacteristic(
                        uuid = ch.uuid,
                        titleAr = MiBand7BleConstants.characteristicDisplayNameAr(ch.uuid),
                        propertiesLabel = props.ifBlank { "—" },
                        canRead = (ch.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0,
                        canWrite = (ch.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0,
                        canNotify = (ch.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                    )
                }
                DiscoveredGattService(
                    uuid = service.uuid,
                    titleAr = MiBand7BleConstants.serviceDisplayNameAr(service.uuid),
                    characteristics = charsUi
                )
            }

            _telemetry.update {
                it.copy(
                    connectionStatus = BleConnectionStatus.CONNECTED,
                    discoveredServices = servicesUi
                )
            }
            appendLog("تم اكتشاف ${servicesUi.size} خدمة GATT")

            // Perform Huami Auth if key provided and 0xFEE1 present
            val authChar = gatt.getService(MiBand7BleConstants.SERVICE_MIBAND_AUTH)
                ?.getCharacteristic(MiBand7BleConstants.CHAR_HUAMI_AUTH)
            val keyBytes = currentAuthKeyBytes
            if (authChar != null && keyBytes != null) {
                startHuamiAuthentication(gatt, authChar)
            } else if (authChar != null) {
                _telemetry.update {
                    it.copy(authStatusMessage = "مفتاح المصادقة (Auth Key) غير مدخل — يتم قراءة الخدمات القياسية فقط")
                }
            }

            queueInitialReads(gatt)
        }

        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val value = characteristic.value ?: ByteArray(0)
            handleCharacteristicPayload(characteristic, value, status)
            finishGattOp()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            handleCharacteristicPayload(characteristic, value, status)
            finishGattOp()
        }

        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: ByteArray(0)
            handleCharacteristicPayload(characteristic, value, BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicPayload(characteristic, value, BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            finishGattOp()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            finishGattOp()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startHuamiAuthentication(gatt: BluetoothGatt, authChar: BluetoothGattCharacteristic) {
        _telemetry.update {
            it.copy(
                connectionStatus = BleConnectionStatus.AUTHENTICATING,
                authStatusMessage = "جارٍ المصادقة مع شاومي باند 7..."
            )
        }
        enableNotifications(gatt, authChar)
        enqueueGattOp {
            // Request challenge nonce from Band: 0x02 0x00
            writeCharacteristicCompat(gatt, authChar, byteArrayOf(0x02, 0x00), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
    }

    @SuppressLint("MissingPermission")
    private fun queueInitialReads(gatt: BluetoothGatt) {
        val standardCharsToRead = listOf(
            MiBand7BleConstants.SERVICE_DEVICE_INFO to MiBand7BleConstants.CHAR_MANUFACTURER,
            MiBand7BleConstants.SERVICE_DEVICE_INFO to MiBand7BleConstants.CHAR_MODEL_NUMBER,
            MiBand7BleConstants.SERVICE_DEVICE_INFO to MiBand7BleConstants.CHAR_SERIAL_NUMBER,
            MiBand7BleConstants.SERVICE_DEVICE_INFO to MiBand7BleConstants.CHAR_FIRMWARE_REV,
            MiBand7BleConstants.SERVICE_DEVICE_INFO to MiBand7BleConstants.CHAR_HARDWARE_REV,
            MiBand7BleConstants.SERVICE_DEVICE_INFO to MiBand7BleConstants.CHAR_SOFTWARE_REV,
            MiBand7BleConstants.SERVICE_BATTERY to MiBand7BleConstants.CHAR_BATTERY_LEVEL,
            MiBand7BleConstants.SERVICE_MIBAND_MAIN to MiBand7BleConstants.CHAR_HUAMI_BATTERY_INFO,
            MiBand7BleConstants.SERVICE_MIBAND_MAIN to MiBand7BleConstants.CHAR_HUAMI_STEPS
        )

        for ((serviceUuid, charUuid) in standardCharsToRead) {
            val ch = gatt.getService(serviceUuid)?.getCharacteristic(charUuid) ?: continue
            if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                enqueueGattOp {
                    if (!gatt.readCharacteristic(ch)) finishGattOp()
                }
            }
        }

        // Enable real-time notifications for Heart Rate, Battery, Huami Steps, Device Events
        val notifyTargets = listOf(
            MiBand7BleConstants.SERVICE_HEART_RATE to MiBand7BleConstants.CHAR_HEART_RATE_MEASUREMENT,
            MiBand7BleConstants.SERVICE_BATTERY to MiBand7BleConstants.CHAR_BATTERY_LEVEL,
            MiBand7BleConstants.SERVICE_MIBAND_MAIN to MiBand7BleConstants.CHAR_HUAMI_STEPS,
            MiBand7BleConstants.SERVICE_MIBAND_MAIN to MiBand7BleConstants.CHAR_HUAMI_BATTERY_INFO,
            MiBand7BleConstants.SERVICE_MIBAND_MAIN to MiBand7BleConstants.CHAR_HUAMI_DEVICE_EVENT
        )
        for ((serviceUuid, charUuid) in notifyTargets) {
            val ch = gatt.getService(serviceUuid)?.getCharacteristic(charUuid) ?: continue
            if ((ch.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {
                enableNotifications(gatt, ch)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        enqueueGattOp {
            gatt.setCharacteristicNotification(characteristic, true)
            val cccd = characteristic.getDescriptor(MiBand7BleConstants.UUID_CCCD)
            if (cccd != null) {
                val value = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val res = gatt.writeDescriptor(cccd, value)
                    if (res != BluetoothGatt.GATT_SUCCESS) finishGattOp()
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = value
                    @Suppress("DEPRECATION")
                    if (!gatt.writeDescriptor(cccd)) finishGattOp()
                }
            } else {
                finishGattOp()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray,
        writeType: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val res = gatt.writeCharacteristic(characteristic, payload, writeType)
            if (res != BluetoothGatt.GATT_SUCCESS) finishGattOp()
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            if (!gatt.writeCharacteristic(characteristic)) finishGattOp()
        }
    }

    private fun handleCharacteristicPayload(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS || value.isEmpty()) return

        val hex = MiBand7BleConstants.bytesToHex(value)
        var decodedSummary = hex

        when (characteristic.uuid) {
            MiBand7BleConstants.CHAR_MANUFACTURER -> {
                val str = value.decodeToString().trim('\u0000')
                decodedSummary = str
                _telemetry.update { it.copy(manufacturer = str) }
            }

            MiBand7BleConstants.CHAR_MODEL_NUMBER -> {
                val str = value.decodeToString().trim('\u0000')
                decodedSummary = str
                _telemetry.update { it.copy(modelNumber = str) }
            }

            MiBand7BleConstants.CHAR_SERIAL_NUMBER -> {
                val str = value.decodeToString().trim('\u0000')
                decodedSummary = str
                _telemetry.update { it.copy(serialNumber = str) }
                emitDeviceInfoSnapshot()
            }

            MiBand7BleConstants.CHAR_FIRMWARE_REV -> {
                val str = value.decodeToString().trim('\u0000')
                decodedSummary = str
                _telemetry.update { it.copy(firmwareVersion = str) }
                emitDeviceInfoSnapshot()
            }

            MiBand7BleConstants.CHAR_HARDWARE_REV -> {
                val str = value.decodeToString().trim('\u0000')
                decodedSummary = str
                _telemetry.update { it.copy(hardwareVersion = str) }
                emitDeviceInfoSnapshot()
            }

            MiBand7BleConstants.CHAR_SOFTWARE_REV -> {
                val str = value.decodeToString().trim('\u0000')
                decodedSummary = str
                _telemetry.update { it.copy(softwareVersion = str) }
            }

            MiBand7BleConstants.CHAR_BATTERY_LEVEL -> {
                val pct = value[0].toInt() and 0xFF
                decodedSummary = "$pct%"
                _telemetry.update { it.copy(batteryLevel = pct.coerceIn(0, 100)) }
                _events.tryEmit(BandTelemetryEvent.BatteryReceived(pct, _telemetry.value.isCharging, ""))
            }

            MiBand7BleConstants.CHAR_HEART_RATE_MEASUREMENT -> {
                val bpm = MiBand7BleConstants.decodeStandardHeartRate(value)
                if (bpm != null && bpm in 30..240) {
                    decodedSummary = "$bpm BPM"
                    _telemetry.update { it.copy(liveHeartRateBpm = bpm) }
                    _events.tryEmit(BandTelemetryEvent.HeartRateReceived(bpm, System.currentTimeMillis()))
                }
            }

            MiBand7BleConstants.CHAR_HUAMI_BATTERY_INFO -> {
                val parsed = MiBand7BleConstants.decodeHuamiBattery(value)
                if (parsed != null) {
                    decodedSummary = "${parsed.level}% (${if (parsed.isCharging) "يشحن" else "غير موصل بالشاحن"})"
                    _telemetry.update {
                        it.copy(
                            batteryLevel = parsed.level,
                            isCharging = parsed.isCharging,
                            lastChargedLevel = parsed.lastChargedLevel,
                            lastChargedDate = parsed.lastChargedFormatted
                        )
                    }
                    _events.tryEmit(
                        BandTelemetryEvent.BatteryReceived(
                            level = parsed.level,
                            isCharging = parsed.isCharging,
                            lastChargedDate = parsed.lastChargedFormatted
                        )
                    )
                }
            }

            MiBand7BleConstants.CHAR_HUAMI_STEPS -> {
                val parsed = MiBand7BleConstants.decodeHuamiSteps(value)
                if (parsed != null) {
                    decodedSummary = "${parsed.steps} خطوة • ${parsed.distanceMeters} م • ${parsed.calories} سعرة"
                    _telemetry.update {
                        it.copy(
                            liveSteps = parsed.steps,
                            liveDistanceMeters = parsed.distanceMeters,
                            liveCalories = parsed.calories
                        )
                    }
                    _events.tryEmit(
                        BandTelemetryEvent.StepsReceived(
                            steps = parsed.steps,
                            distanceMeters = parsed.distanceMeters,
                            calories = parsed.calories
                        )
                    )
                }
            }

            MiBand7BleConstants.CHAR_HUAMI_AUTH -> {
                handleHuamiAuthResponse(characteristic, value)
                decodedSummary = "حزمة مصادقة (${value.size} بايت)"
            }
        }

        // Update GATT Inspector list with latest raw hex and decoded summary
        _telemetry.update { state ->
            val updatedServices = state.discoveredServices.map { svc ->
                svc.copy(
                    characteristics = svc.characteristics.map { ch ->
                        if (ch.uuid == characteristic.uuid) {
                            ch.copy(lastRawHex = hex, decodedValue = decodedSummary)
                        } else {
                            ch
                        }
                    }
                )
            }
            state.copy(discoveredServices = updatedServices)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleHuamiAuthResponse(authChar: BluetoothGattCharacteristic, value: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val keyBytes = currentAuthKeyBytes ?: return
        if (value.size >= 3 && value[0] == 0x10.toByte() && value[1] == 0x02.toByte() && value[2] == 0x01.toByte()) {
            // Received 16-byte random challenge starting at offset 3
            if (value.size >= 19) {
                val challenge = value.copyOfRange(3, 19)
                runCatching {
                    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"))
                    val encrypted = cipher.doFinal(challenge)
                    val response = byteArrayOf(0x03, 0x00) + encrypted
                    enqueueGattOp {
                        writeCharacteristicCompat(gatt, authChar, response, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }.onFailure { err ->
                    _telemetry.update {
                        it.copy(
                            connectionStatus = BleConnectionStatus.CONNECTED,
                            authStatusMessage = "فشل تشفير AES: ${err.localizedMessage}"
                        )
                    }
                }
            }
        } else if (value.size >= 3 && value[0] == 0x10.toByte() && value[1] == 0x03.toByte()) {
            val success = value[2] == 0x01.toByte()
            _telemetry.update {
                it.copy(
                    connectionStatus = BleConnectionStatus.CONNECTED,
                    isAuthenticated = success,
                    authStatusMessage = if (success) {
                        "تمت مصادقة شاومي باند 7 بنجاح!"
                    } else {
                        "فشلت المصادقة — تأكد من صحة مفتاح Auth Key (رمز ${value[2].toInt() and 0xFF})"
                    }
                )
            }
            appendLog(if (success) "نجحت مصادقة شاومي باند 7" else "فشلت مصادقة السوار")
            if (success) {
                queueInitialReads(gatt)
            }
        }
    }

    private fun emitDeviceInfoSnapshot() {
        val t = _telemetry.value
        _events.tryEmit(
            BandTelemetryEvent.DeviceInfoUpdated(
                deviceName = t.connectedName,
                macAddress = t.connectedMac,
                firmware = t.firmwareVersion,
                hardware = t.hardwareVersion,
                serial = t.serialNumber
            )
        )
    }

    private fun enqueueGattOp(op: () -> Unit) {
        synchronized(gattOpQueue) {
            gattOpQueue.addLast(op)
            if (!isGattOpInFlight) {
                isGattOpInFlight = true
                val next = gattOpQueue.removeFirst()
                mainHandler.post { next.invoke() }
            }
        }
    }

    private fun finishGattOp() {
        synchronized(gattOpQueue) {
            if (gattOpQueue.isNotEmpty()) {
                val next = gattOpQueue.removeFirst()
                mainHandler.post { next.invoke() }
            } else {
                isGattOpInFlight = false
            }
        }
    }

    private fun setError(msg: String) {
        _telemetry.update { it.copy(errorMessage = msg) }
        appendLog("تنبيه: $msg")
    }

    private fun appendLog(entry: String) {
        _telemetry.update {
            it.copy(statusLog = (listOf(entry) + it.statusLog).take(25))
        }
    }
}
