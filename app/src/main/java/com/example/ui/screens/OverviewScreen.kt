package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ble.BleConnectionStatus
import com.example.ui.Band7UiState
import com.example.ui.components.ActivityRingsCanvas
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.DateSelectorRow
import com.example.ui.components.EmptyTelemetryNotice
import com.example.ui.components.MetricStatTile
import com.example.ui.components.TelemetryCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MiOrange
import com.example.ui.theme.PulseRose
import com.example.ui.theme.RecoveryEmerald
import com.example.ui.theme.SleepViolet
import com.example.ui.theme.StressAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverviewScreen(
    uiState: Band7UiState,
    onSelectDate: (String) -> Unit,
    onNavigateToDevice: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onSyncNow: () -> Unit,
    onFindBand: () -> Unit
) {
    val summary = uiState.dailySummary
    val live = uiState.liveTelemetry
    val isConnected = live.connectionStatus == BleConnectionStatus.CONNECTED

    val steps = maxOf(summary?.steps ?: 0, if (isConnected) live.liveSteps else 0)
    val stepGoal = summary?.stepGoal ?: uiState.userPrefs.stepGoal
    val distanceMeters = maxOf(summary?.distanceMeters ?: 0, if (isConnected) live.liveDistanceMeters else 0)
    val calories = maxOf(summary?.activeCalories ?: 0, if (isConnected) live.liveCalories else 0)
    val standingHours = summary?.standingHours ?: 0
    val activeMinutes = summary?.activeMinutes ?: 0
    val paiScore = summary?.paiScore ?: 0
    val battery = when {
        live.batteryLevel >= 0 -> live.batteryLevel
        (summary?.batteryLevel ?: -1) >= 0 -> summary?.batteryLevel ?: -1
        else -> -1
    }
    val isCharging = live.isCharging || (summary?.isCharging == true)

    val latestHr = when {
        live.liveHeartRateBpm > 0 -> live.liveHeartRateBpm
        uiState.heartRateSamples.isNotEmpty() -> uiState.heartRateSamples.last().bpm
        (summary?.avgHr ?: 0) > 0 -> summary?.avgHr ?: 0
        else -> 0
    }
    val spo2 = summary?.latestSpo2?.takeIf { it > 0 }
        ?: uiState.spo2Samples.lastOrNull()?.percentage ?: 0
    val stress = summary?.avgStress?.takeIf { it > 0 }
        ?: uiState.stressSamples.lastOrNull()?.level ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Band 7 Device Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_band7_hero),
                    contentDescription = "صورة سوار شاومي باند 7",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xE8070B12),
                                    Color(0x99070B12),
                                    Color(0x33070B12)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConnectionStatusBadge(status = live.connectionStatus)

                        if (battery >= 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xCC111827)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                                        contentDescription = "البطارية",
                                        tint = if (battery <= 20) PulseRose else RecoveryEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$battery%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = live.connectedName.ifBlank {
                                summary?.deviceName?.ifBlank { "شاومي سمارت باند 7" } ?: "شاومي سمارت باند 7"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        val mac = live.connectedMac.ifBlank {
                            summary?.macAddress.orEmpty().ifBlank { uiState.userPrefs.pairedMacAddress }
                        }
                        val fw = live.firmwareVersion.ifBlank { summary?.firmwareVersion.orEmpty() }
                        Text(
                            text = buildString {
                                append(if (mac.isNotBlank()) mac else "غير مقترن بعد")
                                if (fw.isNotBlank()) append(" • v$fw")
                                if (live.rssi != 0 && isConnected) append(" • ${live.rssi} dBm")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isConnected) {
                            Button(
                                onClick = onSyncNow,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("sync_now_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مزامنة")
                            }
                            FilledTonalButton(
                                onClick = onFindBand,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("find_band_button")
                            ) {
                                Icon(Icons.Rounded.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("اهتزاز السوار")
                            }
                        } else {
                            Button(
                                onClick = onNavigateToDevice,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("connect_band_cta"),
                                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                            ) {
                                Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ربط السوار أو استيراد بيانات باند 7")
                            }
                        }
                    }
                }
            }
        }

        // Date Picker Pills
        if (uiState.availableDateKeys.isNotEmpty()) {
            item {
                DateSelectorRow(
                    dates = uiState.availableDateKeys,
                    selectedDate = uiState.selectedDateKey,
                    onSelectDate = onSelectDate
                )
            }
        }

        // Empty State if no data at all yet
        if (summary == null && !isConnected && uiState.sleepSession == null && uiState.workouts.isEmpty()) {
            item {
                EmptyTelemetryNotice(
                    icon = Icons.Rounded.Watch,
                    title = "لا توجد بيانات متزامنة بعد",
                    subtitle = "قم بمسح البلوتوث للاتصال المباشر بسوار Xiaomi Smart Band 7، أو استورد ملف تصدير بيانات Zepp / Mi Fitness لعرض كافة إحصائياتك فوراً.",
                    actionLabel = "إدارة اتصال السوار والاستيراد",
                    actionTag = "overview_empty_connect_btn",
                    onAction = onNavigateToDevice
                )
            }
        }

        // Activity Rings & Daily Goals Card
        item {
            TelemetryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "النشاط اليومي",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    summary?.syncedAtMillis?.let { ts ->
                        Text(
                            text = "آخر تحديث: ${formatTimeShort(ts)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActivityRingsCanvas(
                        stepsProgress = if (stepGoal > 0) steps.toFloat() / stepGoal else 0f,
                        caloriesProgress = calories / 500f,
                        standingProgress = standingHours / 12f,
                        centerPrimaryLabel = if (steps > 0) "$steps" else "--",
                        centerSecondaryLabel = "من $stepGoal خطوة"
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RingLegendRow(
                            label = "الخطوات",
                            value = if (steps > 0) "$steps خطوة" else "--",
                            color = ElectricCyan
                        )
                        RingLegendRow(
                            label = "السعرات النشطة",
                            value = if (calories > 0) "$calories سعرة" else "--",
                            color = MiOrange
                        )
                        RingLegendRow(
                            label = "ساعات الوقوف",
                            value = if (standingHours > 0) "$standingHours / 12 س" else "--",
                            color = SleepViolet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        title = "المسافة",
                        value = if (distanceMeters > 0) String.format(Locale.US, "%.2f", distanceMeters / 1000f) else "--",
                        unit = "كم",
                        icon = Icons.Rounded.Route,
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        title = "الدقائق النشطة",
                        value = if (activeMinutes > 0) "$activeMinutes" else "--",
                        unit = "دقيقة",
                        icon = Icons.Rounded.Timer,
                        accentColor = RecoveryEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // PAI (Personal Activity Intelligence) Card
        item {
            TelemetryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "مؤشر PAI للذكاء الشخصي",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "المستهدف أسبوعياً: 100 نقطة للحفاظ على صحة القلب",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RecoveryEmerald.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (paiScore > 0) "$paiScore" else "--",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RecoveryEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (paiScore / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = RecoveryEmerald,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                if ((summary?.paiToday ?: 0) > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "المكتسب اليوم: +${summary?.paiToday} نقطة",
                        style = MaterialTheme.typography.labelMedium,
                        color = RecoveryEmerald
                    )
                }
            }
        }

        // Quick Vitals & Sleep Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "مؤشرات الصحة والتعافي",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        title = "نبض القلب",
                        value = if (latestHr > 0) "$latestHr" else "--",
                        unit = "ن/د",
                        icon = Icons.Rounded.Favorite,
                        accentColor = PulseRose,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVitals
                    )
                    MetricStatTile(
                        title = "أكسجين الدم SpO2",
                        value = if (spo2 > 0) "$spo2" else "--",
                        unit = "%",
                        icon = Icons.Rounded.Air,
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVitals
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        title = "مستوى التوتر",
                        value = if (stress > 0) "$stress" else "--",
                        unit = "/ 100",
                        icon = Icons.Rounded.Psychology,
                        accentColor = StressAmber,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVitals
                    )
                    val sleepMin = uiState.sleepSession?.totalMinutes ?: 0
                    MetricStatTile(
                        title = "مدة النوم",
                        value = if (sleepMin > 0) "${sleepMin / 60}س ${sleepMin % 60}د" else "--",
                        unit = "",
                        icon = Icons.Rounded.Bedtime,
                        accentColor = SleepViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSleep
                    )
                }
            }
        }
    }
}

@Composable
private fun RingLegendRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimeShort(millis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
}
