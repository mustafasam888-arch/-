package com.example.data.importer

import android.content.Context
import android.net.Uri
import com.example.data.local.Band7Dao
import com.example.data.local.DailySummaryEntity
import com.example.data.local.HeartRateSampleEntity
import com.example.data.local.SleepSessionEntity
import com.example.data.local.Spo2SampleEntity
import com.example.data.local.StressSampleEntity
import com.example.data.local.WorkoutEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportResult(
    val importedRecords: Int,
    val detectedFormat: String
)

class BandExportParser(private val dao: Band7Dao) {

    suspend fun importFromUri(context: Context, uri: Uri, stepGoal: Int): Result<ImportResult> {
        return runCatching {
            val rawText = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: error("تعذر قراءة الملف المحدد")

            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) error("الملف فارغ")

            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                parseJsonExport(trimmed, stepGoal)
            } else {
                parseCsvExport(trimmed, stepGoal)
            }
        }
    }

    private suspend fun parseJsonExport(text: String, stepGoal: Int): ImportResult {
        var count = 0
        val root = if (text.startsWith("[")) {
            JSONObject().apply { put("dailySummaries", JSONArray(text)) }
        } else {
            JSONObject(text)
        }

        val summaries = root.optJSONArray("dailySummaries")
            ?: root.optJSONArray("activity")
            ?: root.optJSONArray("data")
        if (summaries != null) {
            for (i in 0 until summaries.length()) {
                val obj = summaries.optJSONObject(i) ?: continue
                val dateKey = obj.optString("date", obj.optString("dateKey", todayKey()))
                val summary = DailySummaryEntity(
                    dateKey = dateKey,
                    steps = obj.optInt("steps", 0),
                    stepGoal = obj.optInt("stepGoal", stepGoal),
                    distanceMeters = obj.optInt("distanceMeters", obj.optInt("distance", 0)),
                    activeCalories = obj.optInt("activeCalories", obj.optInt("calories", 0)),
                    totalCalories = obj.optInt("totalCalories", obj.optInt("calories", 0)),
                    activeMinutes = obj.optInt("activeMinutes", 0),
                    standingHours = obj.optInt("standingHours", 0),
                    paiScore = obj.optInt("paiScore", obj.optInt("pai", 0)),
                    paiToday = obj.optInt("paiToday", 0),
                    restingHr = obj.optInt("restingHr", 0),
                    avgHr = obj.optInt("avgHr", 0),
                    minHr = obj.optInt("minHr", 0),
                    maxHr = obj.optInt("maxHr", 0),
                    latestSpo2 = obj.optInt("latestSpo2", obj.optInt("spo2", 0)),
                    minSpo2 = obj.optInt("minSpo2", 0),
                    maxSpo2 = obj.optInt("maxSpo2", 0),
                    avgStress = obj.optInt("avgStress", obj.optInt("stress", 0)),
                    minStress = obj.optInt("minStress", 0),
                    maxStress = obj.optInt("maxStress", 0),
                    batteryLevel = obj.optInt("batteryLevel", -1),
                    isCharging = obj.optBoolean("isCharging", false),
                    deviceName = obj.optString("deviceName", "Xiaomi Smart Band 7"),
                    macAddress = obj.optString("macAddress", ""),
                    firmwareVersion = obj.optString("firmwareVersion", ""),
                    syncedAtMillis = System.currentTimeMillis()
                )
                dao.upsertDailySummary(summary)
                count++
            }
        }

        val hrArray = root.optJSONArray("heartRate") ?: root.optJSONArray("heartRateSamples")
        if (hrArray != null) {
            val hrEntities = mutableListOf<HeartRateSampleEntity>()
            for (i in 0 until hrArray.length()) {
                val obj = hrArray.optJSONObject(i) ?: continue
                val bpm = obj.optInt("bpm", obj.optInt("heartRate", 0))
                if (bpm in 30..240) {
                    val ts = obj.optLong("timestampMillis", System.currentTimeMillis())
                    val dk = obj.optString("dateKey", dateKeyFromMillis(ts))
                    hrEntities.add(HeartRateSampleEntity(dateKey = dk, timestampMillis = ts, bpm = bpm))
                }
            }
            if (hrEntities.isNotEmpty()) {
                dao.insertHeartRateSamples(hrEntities)
                count += hrEntities.size
            }
        }

        val spo2Array = root.optJSONArray("spo2") ?: root.optJSONArray("spo2Samples")
        if (spo2Array != null) {
            val spo2Entities = mutableListOf<Spo2SampleEntity>()
            for (i in 0 until spo2Array.length()) {
                val obj = spo2Array.optJSONObject(i) ?: continue
                val pct = obj.optInt("percentage", obj.optInt("spo2", 0))
                if (pct in 70..100) {
                    val ts = obj.optLong("timestampMillis", System.currentTimeMillis())
                    val dk = obj.optString("dateKey", dateKeyFromMillis(ts))
                    spo2Entities.add(Spo2SampleEntity(dateKey = dk, timestampMillis = ts, percentage = pct))
                }
            }
            if (spo2Entities.isNotEmpty()) {
                dao.insertSpo2Samples(spo2Entities)
                count += spo2Entities.size
            }
        }

        val stressArray = root.optJSONArray("stress") ?: root.optJSONArray("stressSamples")
        if (stressArray != null) {
            val stressEntities = mutableListOf<StressSampleEntity>()
            for (i in 0 until stressArray.length()) {
                val obj = stressArray.optJSONObject(i) ?: continue
                val lvl = obj.optInt("level", obj.optInt("stress", 0))
                if (lvl in 1..100) {
                    val ts = obj.optLong("timestampMillis", System.currentTimeMillis())
                    val dk = obj.optString("dateKey", dateKeyFromMillis(ts))
                    stressEntities.add(StressSampleEntity(dateKey = dk, timestampMillis = ts, level = lvl))
                }
            }
            if (stressEntities.isNotEmpty()) {
                dao.insertStressSamples(stressEntities)
                count += stressEntities.size
            }
        }

        val sleepArray = root.optJSONArray("sleep") ?: root.optJSONArray("sleepSessions")
        if (sleepArray != null) {
            val sleepEntities = mutableListOf<SleepSessionEntity>()
            for (i in 0 until sleepArray.length()) {
                val obj = sleepArray.optJSONObject(i) ?: continue
                val deep = obj.optInt("deepMinutes", obj.optInt("deepSleepTime", 0))
                val light = obj.optInt("lightMinutes", obj.optInt("shallowSleepTime", 0))
                val rem = obj.optInt("remMinutes", obj.optInt("REMTime", 0))
                val awake = obj.optInt("awakeMinutes", obj.optInt("wakeTime", 0))
                val total = obj.optInt("totalMinutes", deep + light + rem)
                val dk = obj.optString("dateKey", obj.optString("date", todayKey()))
                if (total > 0) {
                    sleepEntities.add(
                        SleepSessionEntity(
                            dateKey = dk,
                            startMillis = obj.optLong("startMillis", System.currentTimeMillis() - total * 60_000L),
                            endMillis = obj.optLong("endMillis", System.currentTimeMillis()),
                            totalMinutes = total,
                            deepMinutes = deep,
                            lightMinutes = light,
                            remMinutes = rem,
                            awakeMinutes = awake,
                            napMinutes = obj.optInt("napMinutes", 0),
                            sleepScore = obj.optInt("sleepScore", 0),
                            breathingScore = obj.optInt("breathingScore", 0),
                            stagesEncoded = obj.optString("stagesEncoded", "")
                        )
                    )
                }
            }
            if (sleepEntities.isNotEmpty()) {
                dao.insertSleepSessions(sleepEntities)
                count += sleepEntities.size
            }
        }

        val workoutsArray = root.optJSONArray("workouts") ?: root.optJSONArray("sports")
        if (workoutsArray != null) {
            val workoutEntities = mutableListOf<WorkoutEntity>()
            for (i in 0 until workoutsArray.length()) {
                val obj = workoutsArray.optJSONObject(i) ?: continue
                val type = obj.optString("workoutType", obj.optString("type", "RUNNING"))
                workoutEntities.add(
                    WorkoutEntity(
                        dateKey = obj.optString("dateKey", todayKey()),
                        workoutType = type,
                        titleAr = obj.optString("titleAr", translateWorkoutType(type)),
                        startMillis = obj.optLong("startMillis", System.currentTimeMillis()),
                        durationSeconds = obj.optInt("durationSeconds", obj.optInt("sportTime", 0)),
                        distanceMeters = obj.optInt("distanceMeters", obj.optInt("distance", 0)),
                        calories = obj.optInt("calories", 0),
                        avgHr = obj.optInt("avgHr", 0),
                        maxHr = obj.optInt("maxHr", 0),
                        avgPaceSecPerKm = obj.optInt("avgPaceSecPerKm", 0),
                        aerobicTe = obj.optDouble("aerobicTe", 0.0).toFloat(),
                        anaerobicTe = obj.optDouble("anaerobicTe", 0.0).toFloat(),
                        vo2Max = obj.optInt("vo2Max", 0),
                        steps = obj.optInt("steps", 0)
                    )
                )
            }
            if (workoutEntities.isNotEmpty()) {
                dao.insertWorkouts(workoutEntities)
                count += workoutEntities.size
            }
        }

        if (count == 0) error("لم يتم العثور على حقول متوافقة مع شاومي باند 7 داخل ملف JSON")
        return ImportResult(count, "JSON (Xiaomi / Zepp)")
    }

    private suspend fun parseCsvExport(text: String, stepGoal: Int): ImportResult {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) error("ملف CSV لا يحتوي على صفوف بيانات")
        val headers = lines.first().split(",").map { it.trim().lowercase(Locale.ROOT) }
        var count = 0

        when {
            headers.any { it.contains("steps") } -> {
                val dateIdx = headers.indexOfFirst { it == "date" || it == "time" }
                val stepsIdx = headers.indexOfFirst { it == "steps" }
                val distIdx = headers.indexOfFirst { it.contains("distance") }
                val calIdx = headers.indexOfFirst { it.contains("calories") }
                val runDistIdx = headers.indexOfFirst { it.contains("rundistance") }

                for (line in lines.drop(1)) {
                    val cols = line.split(",")
                    val dateKey = cols.getOrNull(dateIdx)?.trim()?.take(10) ?: continue
                    val steps = cols.getOrNull(stepsIdx)?.trim()?.toIntOrNull() ?: 0
                    val dist = cols.getOrNull(distIdx)?.trim()?.toIntOrNull()
                        ?: cols.getOrNull(runDistIdx)?.trim()?.toIntOrNull() ?: 0
                    val cal = cols.getOrNull(calIdx)?.trim()?.toIntOrNull() ?: 0
                    val existing = dao.getDailySummary(dateKey)
                    dao.upsertDailySummary(
                        (existing ?: DailySummaryEntity(dateKey = dateKey, stepGoal = stepGoal)).copy(
                            steps = steps,
                            distanceMeters = dist,
                            activeCalories = cal,
                            totalCalories = cal,
                            syncedAtMillis = System.currentTimeMillis()
                        )
                    )
                    count++
                }
                return ImportResult(count, "Zepp ACTIVITY CSV")
            }

            headers.any { it.contains("heartrate") || it == "bpm" } -> {
                val timeIdx = headers.indexOfFirst { it.contains("time") || it.contains("date") }
                val hrIdx = headers.indexOfFirst { it.contains("heartrate") || it == "bpm" }
                val samples = mutableListOf<HeartRateSampleEntity>()
                for (line in lines.drop(1)) {
                    val cols = line.split(",")
                    val timeRaw = cols.getOrNull(timeIdx)?.trim() ?: continue
                    val bpm = cols.getOrNull(hrIdx)?.trim()?.toIntOrNull() ?: continue
                    if (bpm in 30..240) {
                        val ts = parseTimestamp(timeRaw)
                        samples.add(
                            HeartRateSampleEntity(
                                dateKey = dateKeyFromMillis(ts),
                                timestampMillis = ts,
                                bpm = bpm
                            )
                        )
                    }
                }
                if (samples.isNotEmpty()) {
                    dao.insertHeartRateSamples(samples)
                    val grouped = samples.groupBy { it.dateKey }
                    for ((dk, daySamples) in grouped) {
                        val existing = dao.getDailySummary(dk) ?: DailySummaryEntity(dateKey = dk, stepGoal = stepGoal)
                        dao.upsertDailySummary(
                            existing.copy(
                                avgHr = daySamples.map { it.bpm }.average().toInt(),
                                minHr = daySamples.minOf { it.bpm },
                                maxHr = daySamples.maxOf { it.bpm },
                                syncedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    count = samples.size
                }
                return ImportResult(count, "Zepp HEARTRATE CSV")
            }

            headers.any { it.contains("deepsleeptime") || it.contains("shallowsleeptime") } -> {
                val dateIdx = headers.indexOfFirst { it == "date" }
                val deepIdx = headers.indexOfFirst { it.contains("deepsleeptime") }
                val lightIdx = headers.indexOfFirst { it.contains("shallowsleeptime") }
                val remIdx = headers.indexOfFirst { it.contains("rem") }
                val wakeIdx = headers.indexOfFirst { it.contains("waketime") }
                val startIdx = headers.indexOfFirst { it == "start" }
                val stopIdx = headers.indexOfFirst { it == "stop" }

                val sessions = mutableListOf<SleepSessionEntity>()
                for (line in lines.drop(1)) {
                    val cols = line.split(",")
                    val dk = cols.getOrNull(dateIdx)?.trim()?.take(10) ?: continue
                    val deep = cols.getOrNull(deepIdx)?.trim()?.toIntOrNull() ?: 0
                    val light = cols.getOrNull(lightIdx)?.trim()?.toIntOrNull() ?: 0
                    val rem = cols.getOrNull(remIdx)?.trim()?.toIntOrNull() ?: 0
                    val wake = cols.getOrNull(wakeIdx)?.trim()?.toIntOrNull() ?: 0
                    val total = deep + light + rem
                    if (total > 0) {
                        val startMs = cols.getOrNull(startIdx)?.trim()?.let { parseTimestamp(it) }
                            ?: (System.currentTimeMillis() - total * 60_000L)
                        val endMs = cols.getOrNull(stopIdx)?.trim()?.let { parseTimestamp(it) }
                            ?: System.currentTimeMillis()
                        sessions.add(
                            SleepSessionEntity(
                                dateKey = dk,
                                startMillis = startMs,
                                endMillis = endMs,
                                totalMinutes = total,
                                deepMinutes = deep,
                                lightMinutes = light,
                                remMinutes = rem,
                                awakeMinutes = wake
                            )
                        )
                    }
                }
                if (sessions.isNotEmpty()) {
                    dao.insertSleepSessions(sessions)
                    count = sessions.size
                }
                return ImportResult(count, "Zepp SLEEP CSV")
            }

            headers.any { it.contains("sporttime") || it.contains("workout") } -> {
                val typeIdx = headers.indexOfFirst { it == "type" }
                val startIdx = headers.indexOfFirst { it.contains("starttime") }
                val durIdx = headers.indexOfFirst { it.contains("sporttime") || it.contains("duration") }
                val distIdx = headers.indexOfFirst { it.contains("distance") }
                val calIdx = headers.indexOfFirst { it.contains("calories") }
                val paceIdx = headers.indexOfFirst { it.contains("pace") }

                val workouts = mutableListOf<WorkoutEntity>()
                for (line in lines.drop(1)) {
                    val cols = line.split(",")
                    val type = cols.getOrNull(typeIdx)?.trim() ?: "SPORT"
                    val startMs = cols.getOrNull(startIdx)?.trim()?.let { parseTimestamp(it) }
                        ?: System.currentTimeMillis()
                    val dur = cols.getOrNull(durIdx)?.trim()?.toIntOrNull() ?: 0
                    val dist = cols.getOrNull(distIdx)?.trim()?.toFloatOrNull()?.toInt() ?: 0
                    val cal = cols.getOrNull(calIdx)?.trim()?.toFloatOrNull()?.toInt() ?: 0
                    val pace = cols.getOrNull(paceIdx)?.trim()?.toFloatOrNull()?.toInt() ?: 0
                    if (dur > 0) {
                        workouts.add(
                            WorkoutEntity(
                                dateKey = dateKeyFromMillis(startMs),
                                workoutType = type,
                                titleAr = translateWorkoutType(type),
                                startMillis = startMs,
                                durationSeconds = dur,
                                distanceMeters = dist,
                                calories = cal,
                                avgHr = 0,
                                maxHr = 0,
                                avgPaceSecPerKm = pace
                            )
                        )
                    }
                }
                if (workouts.isNotEmpty()) {
                    dao.insertWorkouts(workouts)
                    count = workouts.size
                }
                return ImportResult(count, "Zepp SPORT CSV")
            }

            else -> error("تنسيق CSV غير مدعوم. يدعم التطبيق ملفات ACTIVITY / HEARTRATE / SLEEP / SPORT / JSON")
        }
    }

    private fun parseTimestamp(raw: String): Long {
        raw.toLongOrNull()?.let { num ->
            return if (num < 10_000_000_000L) num * 1000L else num
        }
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.parse(raw)?.time?.let { return it }
            }
        }
        return System.currentTimeMillis()
    }

    companion object {
        fun todayKey(): String = dateKeyFromMillis(System.currentTimeMillis())

        fun dateKeyFromMillis(millis: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
        }

        fun translateWorkoutType(raw: String): String {
            return when (raw.uppercase(Locale.ROOT)) {
                "1", "RUN", "RUNNING", "OUTDOOR_RUNNING" -> "جري خارجي"
                "6", "WALK", "WALKING" -> "مشي"
                "8", "TREADMILL" -> "جري على السير"
                "9", "CYCLING", "OUTDOOR_CYCLING" -> "ركوب الدراجة"
                "10", "INDOOR_CYCLING" -> "دراجة ثابتة"
                "14", "SWIMMING", "POOL_SWIMMING" -> "سباحة"
                "16", "FREESTYLE", "FREE_TRAINING" -> "تمرين حر"
                "21", "ROPE_SKIPPING" -> "القفز بالحبل"
                else -> "تمرين رياضي"
            }
        }
    }
}
