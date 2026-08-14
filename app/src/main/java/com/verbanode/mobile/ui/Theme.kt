package com.verbanode.mobile.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VerbaNodeColors = lightColorScheme(
    primary = Color(0xFF3578F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF244F9F),
    secondary = Color(0xFF6D5DFC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0EDFF),
    onSecondaryContainer = Color(0xFF4A3FB1),
    tertiary = Color(0xFF20B979),
    onTertiary = Color.White,
    background = Color(0xFFEEF4FF),
    onBackground = Color(0xFF24365D),
    surface = Color(0xFFFCFDFF),
    onSurface = Color(0xFF24365D),
    surfaceVariant = Color(0xFFF3F6FB),
    onSurfaceVariant = Color(0xFF7483A6),
    outline = Color(0xFFD7E2F3),
    outlineVariant = Color(0xFFE5ECF7),
    error = Color(0xFFEF5B68),
    onError = Color.White,
    errorContainer = Color(0xFFFFE9EC),
    onErrorContainer = Color(0xFF8C2E37),
    scrim = Color(0xFF24365D),
)

private val VerbaNodeShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VerbaNodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VerbaNodeColors,
        shapes = VerbaNodeShapes,
        typography = Typography(),
        content = content,
    )
}
