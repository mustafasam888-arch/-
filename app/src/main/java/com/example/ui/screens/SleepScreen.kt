package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SleepSessionEntity
import com.example.ui.Band7UiState
import com.example.ui.components.DateSelectorRow
import com.example.ui.components.EmptyTelemetryNotice
import com.example.ui.components.MetricStatTile
import com.example.ui.components.SleepHypnogramCanvas
import com.example.ui.components.SleepScoreArc
import com.example.ui.components.SleepStageSegment
import com.example.ui.components.TelemetryCard
import com.example.ui.theme.RecoveryEmerald
import com.example.ui.theme.SleepIndigo
import com.example.ui.theme.SleepViolet
import com.example.ui.theme.StressAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SleepScreen(
    uiState: Band7UiState,
    onSelectDate: (String) -> Unit,
    onNavigateToDevice: () -> Unit
) {
    val sleep = uiState.sleepSession

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.availableDateKeys.isNotEmpty()) {
            item {
                DateSelectorRow(
                    dates = uiState.availableDateKeys,
                    selectedDate = uiState.selectedDateKey,
                    onSelectDate = onSelectDate
                )
            }
        }

        if (sleep == null) {
            item {
                EmptyTelemetryNotice(
                    icon = Icons.Rounded.Bedtime,
                    title = "لا توجد بيانات نوم لهذا اليوم",
                    subtitle = "يتتبع سوار شاومي باند 7 مراحل النوم العميق والخفيف وحركة العين السريعة (REM) والقيلولة وجودة التنفس.",
                    actionLabel = "ربط السوار أو استيراد سجل النوم",
                    actionTag = "sleep_empty_connect_btn",
                    onAction = onNavigateToDevice
                )
            }
        } else {
            // Sleep Score & Overview Card
            item {
                TelemetryCard(accentBorder = SleepViolet.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        SleepScoreArc(score = sleep.sleepScore)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "إجمالي النوم",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMinutesAr(sleep.totalMinutes),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text("وقت النوم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatTime(sleep.startMillis), style = MaterialTheme.typography.titleSmall)
                                }
                                Column {
                                    Text("الاستيقاظ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatTime(sleep.endMillis), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Hypnogram Card
            item {
                val segments = parseStages(sleep)
                TelemetryCard {
                    Text(
                        text = "مخطط مراحل النوم (Hypnogram)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SleepHypnogramCanvas(segments = segments)
                    Spacer(modifier = Modifier.height(12.dp))

                    val total = sleep.totalMinutes.coerceAtLeast(1).toFloat()
                    SleepStageRow("نوم عميق", sleep.deepMinutes, sleep.deepMinutes / total, Color(0xFF3B82F6))
                    SleepStageRow("نوم خفيف", sleep.lightMinutes, sleep.lightMinutes / total, SleepIndigo)
                    SleepStageRow("حركة العين السريعة (REM)", sleep.remMinutes, sleep.remMinutes / total, SleepViolet)
                    SleepStageRow("فترات الاستيقاظ", sleep.awakeMinutes, sleep.awakeMinutes / total, StressAmber)
                }
            }

            // Breathing Quality & Naps
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        title = "جودة التنفس",
                        value = if (sleep.breathingScore > 0) "${sleep.breathingScore}" else "--",
                        unit = "/ 100",
                        icon = Icons.Rounded.Air,
                        accentColor = RecoveryEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        title = "القيلولة النهارية",
                        value = if (sleep.napMinutes > 0) "${sleep.napMinutes}" else "0",
                        unit = "دقيقة",
                        icon = Icons.Rounded.WbSunny,
                        accentColor = StressAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.allSleepSessions.isNotEmpty()) {
            item {
                Text(
                    text = "سجل ليالي النوم السابقة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(uiState.allSleepSessions, key = { it.id }) { session ->
                TelemetryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SleepViolet.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = SleepViolet, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(session.dateKey, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "عميق ${session.deepMinutes}د • خفيف ${session.lightMinutes}د • REM ${session.remMinutes}د",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SleepViolet.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = formatMinutesAr(session.totalMinutes),
                                style = MaterialTheme.typography.labelLarge,
                                color = SleepViolet,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepStageRow(
    label: String,
    minutes: Int,
    fraction: Float,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "${formatMinutesAr(minutes)} (${(fraction * 100).toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    }
}

private fun parseStages(session: SleepSessionEntity): List<SleepStageSegment> {
    if (session.stagesEncoded.isNotBlank()) {
        val parsed = session.stagesEncoded.split(",").mapNotNull { token ->
            val parts = token.split(":")
            val type = parts.getOrNull(0)?.trim().orEmpty()
            val min = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            if (type.isNotEmpty() && min > 0) SleepStageSegment(type, min) else null
        }
        if (parsed.isNotEmpty()) return parsed
    }
    return buildList {
        if (session.lightMinutes > 0) add(SleepStageSegment("L", session.lightMinutes / 2))
        if (session.deepMinutes > 0) add(SleepStageSegment("D", session.deepMinutes))
        if (session.remMinutes > 0) add(SleepStageSegment("R", session.remMinutes))
        if (session.lightMinutes > 0) add(SleepStageSegment("L", session.lightMinutes - session.lightMinutes / 2))
        if (session.awakeMinutes > 0) add(SleepStageSegment("A", session.awakeMinutes))
    }
}

private fun formatMinutesAr(minutes: Int): String = "${minutes / 60}س ${minutes % 60}د"

private fun formatTime(millis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
}
