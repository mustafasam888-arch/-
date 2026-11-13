package com.example.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object MiBand7BleConstants {
    // Client Characteristic Configuration Descriptor (CCCD)
    val UUID_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Standard Bluetooth SIG Services & Characteristics
    val SERVICE_GENERIC_ACCESS: UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val CHAR_DEVICE_NAME: UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

    val SERVICE_IMMEDIATE_ALERT: UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb")
    val CHAR_ALERT_LEVEL: UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb")

    val SERVICE_DEVICE_INFO: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    val CHAR_MODEL_NUMBER: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    val CHAR_SERIAL_NUMBER: UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    val CHAR_FIRMWARE_REV: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    val CHAR_HARDWARE_REV: UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
    val CHAR_SOFTWARE_REV: UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")
    val CHAR_MANUFACTURER: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")

    val SERVICE_HEART_RATE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    val CHAR_HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    val CHAR_HEART_RATE_CONTROL: UUID = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb")

    val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    val SERVICE_CURRENT_TIME: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
    val CHAR_CURRENT_TIME: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")

    // Huami / Xiaomi Mi Band Proprietary Services
    val SERVICE_MIBAND_MAIN: UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")
    val SERVICE_MIBAND_AUTH: UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb")

    // Huami Characteristics
    val CHAR_HUAMI_CONFIGURATION: UUID = UUID.fromString("00000003-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_ACTIVITY_CONTROL: UUID = UUID.fromString("00000004-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_ACTIVITY_DATA: UUID = UUID.fromString("00000005-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_BATTERY_INFO: UUID = UUID.fromString("00000006-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_STEPS: UUID = UUID.fromString("00000007-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_USER_INFO: UUID = UUID.fromString("00000008-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_AUTH: UUID = UUID.fromString("00000009-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_DEVICE_EVENT: UUID = UUID.fromString("00000010-0000-3512-2118-0009af100700")
    val CHAR_HUAMI_CHUNKED_TRANSFER: UUID = UUID.fromString("00000020-0000-3512-2118-0009af100700")

    fun serviceDisplayNameAr(uuid: UUID): String = when (uuid) {
        SERVICE_GENERIC_ACCESS -> "الوصول العام للجهاز (Generic Access)"
        SERVICE_IMMEDIATE_ALERT -> "التنبيه الفوري / العثور على السوار"
        SERVICE_DEVICE_INFO -> "معلومات الجهاز والنظام (Device Info)"
        SERVICE_HEART_RATE -> "مستشعر نبضات القلب (Heart Rate)"
        SERVICE_BATTERY -> "خدمة البطارية القياسية (Battery)"
        SERVICE_CURRENT_TIME -> "الوقت الحالي (Current Time)"
        SERVICE_MIBAND_MAIN -> "خدمة شاومي باند الرئيسية (0xFEE0)"
        SERVICE_MIBAND_AUTH -> "خدمة مصادقة شاومي باند (0xFEE1)"
        else -> "خدمة BLE (${uuid.toString().substring(4, 8).uppercase()})"
    }

    fun characteristicDisplayNameAr(uuid: UUID): String = when (uuid) {
        CHAR_DEVICE_NAME -> "اسم الجهاز"
        CHAR_ALERT_LEVEL -> "مستوى التنبيه والاهتزاز"
        CHAR_MODEL_NUMBER -> "رقم الطراز"
        CHAR_SERIAL_NUMBER -> "الرقم التسلسلي"
        CHAR_FIRMWARE_REV -> "إصدار البرنامج الثابت (Firmware)"
        CHAR_HARDWARE_REV -> "إصدار العتاد (Hardware)"
        CHAR_SOFTWARE_REV -> "إصدار النظام (Software)"
        CHAR_MANUFACTURER -> "الشركة المصنعة"
        CHAR_HEART_RATE_MEASUREMENT -> "قياس نبضات القلب (BPM)"
        CHAR_HEART_RATE_CONTROL -> "نقطة تحكم النبض"
        CHAR_BATTERY_LEVEL -> "نسبة البطارية"
        CHAR_CURRENT_TIME -> "الوقت والتاريخ"
        CHAR_HUAMI_CONFIGURATION -> "إعدادات السوار (Huami Config)"
        CHAR_HUAMI_ACTIVITY_CONTROL -> "التحكم بمزامنة النشاط"
        CHAR_HUAMI_ACTIVITY_DATA -> "حزم بيانات النشاط والنوم"
        CHAR_HUAMI_BATTERY_INFO -> "تفاصيل البطارية والشحن (Huami)"
        CHAR_HUAMI_STEPS -> "الخطوات والمسافة والسعرات الحية"
        CHAR_HUAMI_USER_INFO -> "بيانات المستخدم"
        CHAR_HUAMI_AUTH -> "قناة المصادقة المشفرة AES"
        CHAR_HUAMI_DEVICE_EVENT -> "أحداث السوار والتفاعل"
        CHAR_HUAMI_CHUNKED_TRANSFER -> "نقل البيانات المجمعة (ZeppOS)"
        else -> "خاصية ${uuid.toString().substring(4, 8).uppercase()}"
    }

    data class HuamiBatteryPayload(
        val level: Int,
        val isCharging: Boolean,
        val lastChargedLevel: Int,
        val lastChargedFormatted: String
    )

    data class HuamiStepsPayload(
        val steps: Int,
        val distanceMeters: Int,
        val calories: Int
    )

    fun decodeStandardHeartRate(bytes: ByteArray): Int? {
        if (bytes.isEmpty()) return null
        val flags = bytes[0].toInt() and 0xFF
        val isUint16 = (flags and 0x01) != 0
        return if (!isUint16 && bytes.size >= 2) {
            bytes[1].toInt() and 0xFF
        } else if (isUint16 && bytes.size >= 3) {
            ByteBuffer.wrap(bytes, 1, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        } else {
            null
        }
    }

    fun decodeHuamiBattery(bytes: ByteArray): HuamiBatteryPayload? {
        // Huami 0x0006 payload is typically 20 bytes:
        // [1] = battery level %, [2] = status (0x00 normal, 0x01 charging),
        // [11..16] = last charge year/month/day/hour/min/sec, [19] = last charge level
        if (bytes.size < 3) return null
        val level = bytes[1].toInt() and 0xFF
        val isCharging = (bytes[2].toInt() and 0xFF) == 0x01
        val lastLevel = if (bytes.size >= 20) bytes[19].toInt() and 0xFF else 0
        val lastDate = if (bytes.size >= 17) {
            val year = ByteBuffer.wrap(bytes, 11, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            val month = bytes[13].toInt() and 0xFF
            val day = bytes[14].toInt() and 0xFF
            val hour = bytes[15].toInt() and 0xFF
            val min = bytes[16].toInt() and 0xFF
            if (year in 2020..2099 && month in 1..12 && day in 1..31) {
                String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, min)
            } else {
                ""
            }
        } else {
            ""
        }
        return HuamiBatteryPayload(
            level = level.coerceIn(0, 100),
            isCharging = isCharging,
            lastChargedLevel = lastLevel.coerceIn(0, 100),
            lastChargedFormatted = lastDate
        )
    }

    fun decodeHuamiSteps(bytes: ByteArray): HuamiStepsPayload? {
        // Huami 0x0007 payload (13 bytes):
        // [0] unknown/flags, [1..4] steps (uint32 LE or uint16 LE), [5..8] distance (m), [9..12] calories (kcal)
        if (bytes.size < 5) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val steps = if (bytes.size >= 5) buf.getInt(1) else 0
        val distance = if (bytes.size >= 9) buf.getInt(5) else 0
        val calories = if (bytes.size >= 13) buf.getInt(9) else 0
        if (steps < 0 || steps > 200_000) return null
        return HuamiStepsPayload(
            steps = steps,
            distanceMeters = distance.coerceAtLeast(0),
            calories = calories.coerceAtLeast(0)
        )
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it) }

    fun hexStringToBytes(hex: String): ByteArray? {
        val clean = hex.replace(" ", "").replace(":", "").trim()
        if (clean.length % 2 != 0 || clean.isEmpty()) return null
        return runCatching {
            ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }
}
