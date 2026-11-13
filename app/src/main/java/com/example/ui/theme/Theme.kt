package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val Band7DarkColorScheme = darkColorScheme(
    primary = MiOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1C08),
    onPrimaryContainer = MiOrangeGlow,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF031A22),
    secondaryContainer = Color(0xFF0A2E38),
    onSecondaryContainer = ElectricCyan,
    tertiary = SleepViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF261A4A),
    onTertiaryContainer = Color(0xFFD8CCFF),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = DeepSlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedCardSurface,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = DeepSlateSurface,
    surfaceContainer = ElevatedCardSurface,
    surfaceContainerHigh = HighestCardSurface,
    outline = SubtleBorderDark,
    outlineVariant = Color(0xFF1B2638),
    error = PulseRose,
    onError = Color.White
)

private val Band7LightColorScheme = lightColorScheme(
    primary = MiOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5D4),
    onPrimaryContainer = Color(0xFF5C2200),
    secondary = Color(0xFF008BA3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F7FF),
    onSecondaryContainer = Color(0xFF003B47),
    tertiary = SleepViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF3B1D8E),
    background = CrispLightBg,
    onBackground = TextPrimaryLight,
    surface = CrispLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CrispLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = CrispLightSurface,
    surfaceContainer = CrispLightSurface,
    surfaceContainerHigh = CrispLightSurfaceVariant,
    outline = CrispLightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = PulseRose,
    onError = Color.White
)

val Band7Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun Band7Theme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) Band7DarkColorScheme else Band7LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Band7Typography,
            shapes = Band7Shapes,
            content = content
        )
    }
}
