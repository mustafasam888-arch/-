package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MiOrange
import com.example.ui.theme.PulseRose
import com.example.ui.theme.RecoveryEmerald
import com.example.ui.theme.SleepIndigo
import com.example.ui.theme.SleepViolet
import com.example.ui.theme.StressAmber

@Composable
fun ActivityRingsCanvas(
    stepsProgress: Float,
    caloriesProgress: Float,
    standingProgress: Float,
    centerPrimaryLabel: String,
    centerSecondaryLabel: String,
    modifier: Modifier = Modifier
) {
    val animSteps by animateFloatAsState(stepsProgress.coerceIn(0f, 1f), tween(700), label = "stepsRing")
    val animCal by animateFloatAsState(caloriesProgress.coerceIn(0f, 1f), tween(800), label = "calRing")
    val animStand by animateFloatAsState(standingProgress.coerceIn(0f, 1f), tween(900), label = "standRing")
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(modifier = modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val gap = 6.dp.toPx()

            fun drawRing(inset: Float, progress: Float, brush: Brush) {
                val diameter = size.minDimension - inset * 2
                if (diameter <= 0f) return
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (progress > 0f) {
                    drawArc(
                        brush = brush,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            drawRing(
                inset = stroke / 2f,
                progress = animSteps,
                brush = Brush.sweepGradient(listOf(ElectricCyan, RecoveryEmerald, ElectricCyan))
            )
            drawRing(
                inset = stroke * 1.5f + gap,
                progress = animCal,
                brush = Brush.sweepGradient(listOf(MiOrange, PulseRose, MiOrange))
            )
            drawRing(
                inset = stroke * 2.5f + gap * 2f,
                progress = animStand,
                brush = Brush.sweepGradient(listOf(SleepViolet, SleepIndigo, SleepViolet))
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerPrimaryLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerSecondaryLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TelemetryLineChart(
    values: List<Int>,
    lineColor: Color,
    minRange: Int,
    maxRange: Int,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val h = size.height
        val w = size.width
        val paddingV = 12.dp.toPx()
        val usableH = (h - paddingV * 2).coerceAtLeast(1f)

        // Subtle horizontal grid lines
        for (step in 0..3) {
            val y = paddingV + usableH * (step / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (values.isEmpty()) return@Canvas

        val effectiveMin = minOf(minRange, values.minOrNull() ?: minRange)
        val effectiveMax = maxOf(maxRange, values.maxOrNull() ?: maxRange).coerceAtLeast(effectiveMin + 1)
        val span = (effectiveMax - effectiveMin).toFloat()

        val points = values.mapIndexed { idx, value ->
            val x = if (values.size == 1) w / 2f else (idx.toFloat() / (values.size - 1)) * w
            val norm = ((value - effectiveMin) / span).coerceIn(0f, 1f)
            val y = paddingV + usableH * (1f - norm)
            Offset(x, y)
        }

        if (points.size == 1) {
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = points.first())
            return@Canvas
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.34f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SleepScoreArc(
    score: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState((score / 100f).coerceIn(0f, 1f), tween(700), label = "sleepArc")
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(modifier = modifier.size(136.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(SleepIndigo, SleepViolet, SleepIndigo)),
                    startAngle = 140f,
                    sweepAngle = 260f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score > 0) "$score" else "--",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "درجة النوم",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class SleepStageSegment(
    val type: String, // "D", "L", "R", "A"
    val minutes: Int
)

@Composable
fun SleepHypnogramCanvas(
    segments: List<SleepStageSegment>,
    modifier: Modifier = Modifier
) {
    val deepColor = Color(0xFF3B82F6)
    val lightColor = SleepIndigo
    val remColor = SleepViolet
    val awakeColor = StressAmber
    val bgLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
    ) {
        val totalMin = segments.sumOf { it.minutes }.coerceAtLeast(1)
        val rowHeight = size.height / 4f
        for (row in 1..3) {
            val y = row * rowHeight
            drawLine(bgLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        var cursorX = 0f
        for (seg in segments) {
            val segW = (seg.minutes.toFloat() / totalMin) * size.width
            val (rowIdx, color) = when (seg.type) {
                "A" -> 0 to awakeColor
                "R" -> 1 to remColor
                "L" -> 2 to lightColor
                else -> 3 to deepColor
            }
            val top = rowIdx * rowHeight + 4.dp.toPx()
            val barH = (rowHeight - 8.dp.toPx()).coerceAtLeast(6.dp.toPx())
            drawRoundRect(
                color = color,
                topLeft = Offset(cursorX, top),
                size = Size(segW.coerceAtLeast(3.dp.toPx()), barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            cursorX += segW
        }
    }
}
