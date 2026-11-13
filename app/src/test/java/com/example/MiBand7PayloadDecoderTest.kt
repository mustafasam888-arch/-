package com.example

import com.example.ble.MiBand7BleConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiBand7PayloadDecoderTest {

    @Test
    fun decodeStandardHeartRate_8bit_returnsCorrectBpm() {
        val payload = byteArrayOf(0x00, 74.toByte())
        val bpm = MiBand7BleConstants.decodeStandardHeartRate(payload)
        assertEquals(74, bpm)
    }

    @Test
    fun decodeStandardHeartRate_16bit_returnsCorrectBpm() {
        // flags=0x01 (uint16), bpm = 162 (0xA2, 0x00 LE)
        val payload = byteArrayOf(0x01, 0xA2.toByte(), 0x00)
        val bpm = MiBand7BleConstants.decodeStandardHeartRate(payload)
        assertEquals(162, bpm)
    }

    @Test
    fun decodeHuamiSteps_returnsStepsDistanceCalories() {
        // 13 bytes LE: [0]=0x00, steps=6420 (0x14,0x19,0x00,0x00), dist=4500 (0x94,0x11,0x00,0x00), cal=215 (0xD7,0x00,0x00,0x00)
        val payload = byteArrayOf(
            0x00,
            0x14, 0x19, 0x00, 0x00,
            0x94.toByte(), 0x11, 0x00, 0x00,
            0xD7.toByte(), 0x00, 0x00, 0x00
        )
        val decoded = MiBand7BleConstants.decodeHuamiSteps(payload)
        assertNotNull(decoded)
        assertEquals(6420, decoded!!.steps)
        assertEquals(4500, decoded.distanceMeters)
        assertEquals(215, decoded.calories)
    }

    @Test
    fun decodeHuamiBattery_returnsLevelAndCharging() {
        val payload = ByteArray(20).apply {
            this[1] = 84
            this[2] = 1
            this[11] = (2026 and 0xFF).toByte()
            this[12] = (2026 shr 8).toByte()
            this[13] = 11
            this[14] = 13
            this[15] = 9
            this[16] = 30
            this[19] = 100
        }
        val decoded = MiBand7BleConstants.decodeHuamiBattery(payload)
        assertNotNull(decoded)
        assertEquals(84, decoded!!.level)
        assertTrue(decoded.isCharging)
        assertEquals(100, decoded.lastChargedLevel)
        assertEquals("2026-11-13 09:30", decoded.lastChargedFormatted)
    }
}
