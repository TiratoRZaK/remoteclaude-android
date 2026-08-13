package dev.rclaude.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Раскраска терминала текущего оформления. */
val LocalTerminalSkin = staticCompositionLocalOf { AppStyle.DEFAULT.spec().terminal }

/** Выбранное оформление приложения. */
val LocalAppStyle = staticCompositionLocalOf { AppStyle.DEFAULT }

/**
 * Тема приложения по выбранному оформлению: палитра, шрифты, скругления, фон окна и
 * раскраска терминала. Фон рисуется под содержимым, поэтому экраны прозрачные.
 */
@Composable
fun RemoteClaudeTheme(
    style: AppStyle = AppStyle.DEFAULT,
    content: @Composable () -> Unit,
) {
    val spec = remember(style) { style.spec() }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !spec.dark
            controller.isAppearanceLightNavigationBars = !spec.dark
        }
    }
    MaterialTheme(
        colorScheme = spec.colorScheme,
        typography = spec.typography,
        shapes = spec.shapes,
    ) {
        CompositionLocalProvider(
            LocalTerminalSkin provides spec.terminal,
            LocalAppStyle provides style,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(spec.background),
            ) {
                content()
            }
        }
    }
}
