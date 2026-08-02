package com.cyanbudget.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cyanbudget.app.model.AppSettings

val Navy = Color(0xFF0D213A)
val SoftBlue = Color(0xFF377CF6)
val Emerald = Color(0xFF18A77A)
val Coral = Color(0xFFED665B)
val Mist = Color(0xFFF4F7FA)
val DarkSurface = Color(0xFF132B45)

private fun light(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = .13f),
    onPrimaryContainer = Navy,
    secondary = Emerald,
    tertiary = Coral,
    background = Mist,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFEAF0F5),
    onSurfaceVariant = Color(0xFF52667A),
    outline = Color(0xFFB8C5D1),
    error = Color(0xFFB3261E)
)

private fun dark(accent: Color) = darkColorScheme(
    primary = Color(0xFF8CB4FF),
    onPrimary = Color(0xFF002F6A),
    primaryContainer = accent.copy(alpha = .26f),
    secondary = Color(0xFF61DDB8),
    tertiary = Color(0xFFFFA69A),
    background = Color(0xFF071727),
    onBackground = Color(0xFFE3ECF5),
    surface = DarkSurface,
    onSurface = Color(0xFFE3ECF5),
    surfaceVariant = Color(0xFF203950),
    onSurfaceVariant = Color(0xFFB9C9D8),
    outline = Color(0xFF657A8D)
)

@Composable
fun CyanBudgetTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val darkMode = when (settings.theme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    val accent = Color(settings.accent)
    val scheme = if (darkMode) dark(accent) else light(accent)
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = scheme.background.toArgb()
        window.navigationBarColor = scheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkMode
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
