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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.WorkoutEntity
import com.example.ui.Band7UiState
import com.example.ui.components.EmptyTelemetryNotice
import com.example.ui.components.MetricStatTile
import com.example.ui.components.TelemetryCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MiOrange
import com.example.ui.theme.PulseRose
import com.example.ui.theme.RecoveryEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutsScreen(
    uiState: Band7UiState,
    onNavigateToDevice: () -> Unit
) {
    val workouts = uiState.workouts
    val filterCategories = listOf("الكل", "جري", "مشي", "دراجة", "سباحة", "أخرى")
    var selectedFilter by remember { mutableStateOf("الكل") }

    val filtered = remember(workouts, selectedFilter) {
        if (selectedFilter == "الكل") {
            workouts
        } else {
            workouts.filter { workout ->
                when (selectedFilter) {
                    "جري" -> workout.titleAr.contains("جري")
                    "مشي" -> workout.titleAr.contains("مشي")
                    "دراجة" -> workout.titleAr.contains("دراجة")
                    "سباحة" -> workout.titleAr.contains("سباحة")
                    else -> !workout.titleAr.contains("جري") &&
                        !workout.titleAr.contains("مشي") &&
                        !workout.titleAr.contains("دراجة") &&
                        !workout.titleAr.contains("سباحة")
                }
            }
        }
    }

    val totalDistanceKm = workouts.sumOf { it.distanceMeters } / 1000f
    val totalCalories = workouts.sumOf { it.calories }
    val totalMinutes = workouts.sumOf { it.durationSeconds } / 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    title = "إجمالي التمارين",
                    value = "${workouts.size}",
                    unit = "جلسة",
                    icon = Icons.Rounded.FitnessCenter,
                    accentColor = MiOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    title = "المسافة الكلية",
                    value = String.format(Locale.US, "%.1f", totalDistanceKm),
                    unit = "كم",
                    icon = Icons.Rounded.Route,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    title = "السعرات",
                    value = "$totalCalories",
                    unit = "سعرة",
                    icon = Icons.Rounded.LocalFireDepartment,
                    accentColor = PulseRose,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    title = "الوقت الرياضي",
                    value = "$totalMinutes",
                    unit = "دقيقة",
                    icon = Icons.Rounded.Timer,
                    accentColor = RecoveryEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterCategories) { category ->
                    FilterChip(
                        selected = selectedFilter == category,
                        onClick = { selectedFilter = category },
                        label = { Text(category) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workout_filter_$category")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyTelemetryNotice(
                    icon = Icons.Rounded.DirectionsRun,
                    title = "لا توجد تمارين مسجلة",
                    subtitle = "يدعم شاومي باند 7 أكثر من 110 وضع رياضي مع قياس VO2 Max وتأثير التمرين الهوائي واللاهوائي. قم بمزامنة السوار أو استيراد سجل التمارين.",
                    actionLabel = "إدارة الاتصال والاستيراد",
                    actionTag = "workouts_empty_connect_btn",
                    onAction = onNavigateToDevice
                )
            }
        } else {
            items(filtered, key = { it.id }) { workout ->
                WorkoutCard(workout = workout)
            }
        }
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutEntity) {
    val icon = workoutIcon(workout.titleAr)
    TelemetryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MiOrange.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = workout.titleAr, tint = MiOrange, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(workout.titleAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd • HH:mm", Locale.US).format(Date(workout.startMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (workout.vo2Max > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RecoveryEmerald.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "VO2 ${workout.vo2Max}",
                        style = MaterialTheme.typography.labelMedium,
                        color = RecoveryEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WorkoutMetric("المدة", formatDuration(workout.durationSeconds))
            WorkoutMetric(
                "المسافة",
                if (workout.distanceMeters > 0) String.format(Locale.US, "%.2f كم", workout.distanceMeters / 1000f) else "--"
            )
            WorkoutMetric("السعرات", "${workout.calories} سعرة")
            WorkoutMetric("متوسط النبض", if (workout.avgHr > 0) "${workout.avgHr} ن/د" else "--")
        }

        if (workout.aerobicTe > 0f || workout.anaerobicTe > 0f || workout.avgPaceSecPerKm > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (workout.avgPaceSecPerKm > 0) {
                        val pMin = workout.avgPaceSecPerKm / 60
                        val pSec = workout.avgPaceSecPerKm % 60
                        Text(
                            text = "الإيقاع: $pMin'${String.format(Locale.US, "%02d", pSec)}\"/كم",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                    if (workout.aerobicTe > 0f) {
                        Text(
                            text = "هوائي: ${String.format(Locale.US, "%.1f", workout.aerobicTe)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = RecoveryEmerald
                        )
                    }
                    if (workout.anaerobicTe > 0f) {
                        Text(
                            text = "لاهوائي: ${String.format(Locale.US, "%.1f", workout.anaerobicTe)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MiOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

private fun workoutIcon(title: String): ImageVector = when {
    title.contains("جري") -> Icons.Rounded.DirectionsRun
    title.contains("مشي") -> Icons.Rounded.DirectionsWalk
    title.contains("دراجة") -> Icons.Rounded.DirectionsBike
    title.contains("سباحة") -> Icons.Rounded.Pool
    else -> Icons.Rounded.FitnessCenter
}

private fun formatDuration(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
