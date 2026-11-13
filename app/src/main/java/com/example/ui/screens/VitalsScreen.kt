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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Psychology
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
import com.example.ui.Band7UiState
import com.example.ui.components.DateSelectorRow
import com.example.ui.components.EmptyTelemetryNotice
import com.example.ui.components.TelemetryCard
import com.example.ui.components.TelemetryLineChart
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MiOrange
import com.example.ui.theme.PulseRose
import com.example.ui.theme.RecoveryEmerald
import com.example.ui.theme.SleepIndigo
import com.example.ui.theme.StressAmber

@Composable
fun VitalsScreen(
    uiState: Band7UiState,
    onSelectDate: (String) -> Unit,
    onNavigateToDevice: () -> Unit
) {
    val summary = uiState.dailySummary
    val hrSamples = uiState.heartRateSamples.map { it.bpm }
    val spo2Samples = uiState.spo2Samples.map { it.percentage }
    val stressSamples = uiState.stressSamples.map { it.level }

    val liveHr = uiState.liveTelemetry.liveHeartRateBpm
    val currentHr = when {
        liveHr > 0 -> liveHr
        hrSamples.isNotEmpty() -> hrSamples.last()
        else -> summary?.avgHr ?: 0
    }
    val minHr = hrSamples.minOrNull() ?: (summary?.minHr ?: 0)
    val maxHr = hrSamples.maxOrNull() ?: (summary?.maxHr ?: 0)
    val avgHr = if (hrSamples.isNotEmpty()) hrSamples.average().toInt() else (summary?.avgHr ?: 0)
    val restingHr = summary?.restingHr ?: 0

    val latestSpo2 = spo2Samples.lastOrNull() ?: (summary?.latestSpo2 ?: 0)
    val minSpo2 = spo2Samples.minOrNull() ?: (summary?.minSpo2 ?: 0)
    val maxSpo2 = spo2Samples.maxOrNull() ?: (summary?.maxSpo2 ?: 0)

    val latestStress = stressSamples.lastOrNull() ?: (summary?.avgStress ?: 0)
    val minStress = stressSamples.minOrNull() ?: (summary?.minStress ?: 0)
    val maxStress = stressSamples.maxOrNull() ?: (summary?.maxStress ?: 0)

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

        if (currentHr == 0 && latestSpo2 == 0 && latestStress == 0) {
            item {
                EmptyTelemetryNotice(
                    icon = Icons.Rounded.Favorite,
                    title = "لا توجد قراءات حيوية مسجلة لهذا اليوم",
                    subtitle = "عند الاتصال بسوار شاومي باند 7 أو استيراد ملف البيانات، ستظهر هنا قراءات النبض والأكسجين والتوتر ومناطق الجهد القلبي.",
                    actionLabel = "إعداد الاتصال أو الاستيراد",
                    actionTag = "vitals_empty_connect_btn",
                    onAction = onNavigateToDevice
                )
            }
        }

        // Heart Rate Card
        item {
            TelemetryCard(accentBorder = PulseRose.copy(alpha = 0.45f)) {
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
                                .size(38.dp)
                                .background(PulseRose.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = "معدل النبض", tint = PulseRose)
                        }
                        Column {
                            Text("معدل نبضات القلب", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (liveHr > 0) "بث مباشر عبر BLE" else "القياس اليومي المستمر",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (liveHr > 0) RecoveryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (currentHr > 0) "$currentHr" else "--",
                            style = MaterialTheme.typography.displayMedium,
                            color = PulseRose
                        )
                        Text(
                            text = "ن/د",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TelemetryLineChart(
                    values = hrSamples,
                    lineColor = PulseRose,
                    minRange = 45,
                    maxRange = 165
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VitalMiniColumn("الراحة", if (restingHr > 0) "$restingHr" else "--")
                    VitalMiniColumn("المتوسط", if (avgHr > 0) "$avgHr" else "--")
                    VitalMiniColumn("الأدنى", if (minHr > 0) "$minHr" else "--")
                    VitalMiniColumn("الأقصى", if (maxHr > 0) "$maxHr" else "--")
                }

                if (hrSamples.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "توزيع مناطق نبض القلب (${hrSamples.size} عينة)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val total = hrSamples.size.toFloat()
                    val warmup = hrSamples.count { it < 105 } / total
                    val fatBurn = hrSamples.count { it in 105..124 } / total
                    val aerobic = hrSamples.count { it in 125..144 } / total
                    val anaerobic = hrSamples.count { it in 145..164 } / total
                    val vo2 = hrSamples.count { it >= 165 } / total

                    HeartZoneBar("إحماء (<105)", warmup, SleepIndigo)
                    HeartZoneBar("حرق الدهون (105–124)", fatBurn, RecoveryEmerald)
                    HeartZoneBar("هوائي (125–144)", aerobic, StressAmber)
                    HeartZoneBar("لاهوائي (145–164)", anaerobic, MiOrange)
                    HeartZoneBar("أقصى جهد (165+)", vo2, PulseRose)
                }
            }
        }

        // Blood Oxygen (SpO2) Card
        item {
            TelemetryCard(accentBorder = ElectricCyan.copy(alpha = 0.45f)) {
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
                                .size(38.dp)
                                .background(ElectricCyan.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Air, contentDescription = "تشبع الأكسجين", tint = ElectricCyan)
                        }
                        Column {
                            Text("أكسجين الدم (SpO2)", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = when {
                                    latestSpo2 >= 95 -> "ضمن النطاق الطبيعي (95% – 100%)"
                                    latestSpo2 in 90..94 -> "منخفض نسبياً"
                                    latestSpo2 in 1..89 -> "منخفض — يرجى الراحة وإعادة القياس"
                                    else -> "النطاق الصحي: 95% – 100%"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (latestSpo2 > 0) "$latestSpo2%" else "--",
                        style = MaterialTheme.typography.displayMedium,
                        color = ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                TelemetryLineChart(
                    values = spo2Samples,
                    lineColor = ElectricCyan,
                    minRange = 85,
                    maxRange = 100
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VitalMiniColumn("الأدنى اليومي", if (minSpo2 > 0) "$minSpo2%" else "--")
                    VitalMiniColumn("الأعلى اليومي", if (maxSpo2 > 0) "$maxSpo2%" else "--")
                    VitalMiniColumn("عدد القراءات", "${spo2Samples.size}")
                }
            }
        }

        // Stress Level Card
        item {
            TelemetryCard(accentBorder = StressAmber.copy(alpha = 0.45f)) {
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
                                .size(38.dp)
                                .background(StressAmber.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Psychology, contentDescription = "مستوى التوتر", tint = StressAmber)
                        }
                        Column {
                            Text("مستوى التوتر (Stress)", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = stressStatusLabel(latestStress),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StressAmber.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (latestStress > 0) "$latestStress" else "--",
                            style = MaterialTheme.typography.headlineMedium,
                            color = StressAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TelemetryLineChart(
                    values = stressSamples,
                    lineColor = StressAmber,
                    minRange = 0,
                    maxRange = 100
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VitalMiniColumn("الأدنى", if (minStress > 0) "$minStress" else "--")
                    VitalMiniColumn("الأقصى", if (maxStress > 0) "$maxStress" else "--")
                    VitalMiniColumn("التصنيف", stressStatusLabel(latestStress))
                }
            }
        }
    }
}

@Composable
private fun VitalMiniColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeartZoneBar(label: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
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

private fun stressStatusLabel(score: Int): String = when {
    score <= 0 -> "غير مقاس"
    score <= 29 -> "مريح"
    score <= 59 -> "طبيعي"
    score <= 79 -> "متوسط"
    else -> "مرتفع"
}
