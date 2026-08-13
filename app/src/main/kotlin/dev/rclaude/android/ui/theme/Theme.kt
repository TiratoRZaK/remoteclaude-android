package dev.rclaude.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF4FC3F7)
private val AccentDark = Color(0xFF0079C2)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF06232F),
    primaryContainer = Color(0xFF13405A),
    onPrimaryContainer = Color(0xFFD6EEFA),
    secondary = Color(0xFF9BB3C2),
    background = Color(0xFF10141A),
    onBackground = Color(0xFFE3E7EB),
    surface = Color(0xFF161C24),
    onSurface = Color(0xFFE3E7EB),
    surfaceVariant = Color(0xFF232C36),
    onSurfaceVariant = Color(0xFFC3CBD3),
)

private val LightScheme = lightColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EAF8),
    onPrimaryContainer = Color(0xFF002438),
    secondary = Color(0xFF4C6273),
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF171C21),
    surface = Color.White,
    onSurface = Color(0xFF171C21),
    surfaceVariant = Color(0xFFE1E7EC),
    onSurfaceVariant = Color(0xFF42484E),
)

/** Тема приложения: тёмная по системной настройке, акцент — синий как у веб-клиента. */
@Composable
fun RemoteClaudeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
