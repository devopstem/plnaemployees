package by.smartplanogram.spmob.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import by.smartplanogram.spmob.data.prefs.AppThemeMode

private val SpDarkColors = darkColorScheme(
    primary = SpPurple,
    onPrimary = Color.White,
    primaryContainer = SpPurpleContainer,
    onPrimaryContainer = SpOnSurface,
    secondary = SpCyan,
    onSecondary = SpBg,
    secondaryContainer = Color(0xFF163A3A),
    onSecondaryContainer = SpOnSurface,
    background = SpBg,
    onBackground = SpOnSurface,
    surface = SpSurface,
    onSurface = SpOnSurface,
    surfaceVariant = SpSurfaceVariant,
    onSurfaceVariant = SpOnSurfaceVariant,
    outline = SpOutline,
    error = SpError,
    onError = Color.White,
)

private val SpLightColors = lightColorScheme(
    primary = SpPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E4FF),
    onPrimaryContainer = Color(0xFF1A1438),
    secondary = SpCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F5F1),
    onSecondaryContainer = Color(0xFF06332E),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF12151C),
    surface = Color.White,
    onSurface = Color(0xFF12151C),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFFD0D5DE),
    error = SpError,
    onError = Color.White,
)

@Composable
fun SpTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) SpDarkColors else SpLightColors,
        typography = SpTypography,
        content = content,
    )
}
