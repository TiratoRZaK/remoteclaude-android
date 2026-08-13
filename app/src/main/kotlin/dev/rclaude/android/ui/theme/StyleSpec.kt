package dev.rclaude.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import dev.rclaude.android.R

/** Всё оформление одного стиля: палитра, шрифты, скругления, фон и вид терминала. */
data class StyleSpec(
    val dark: Boolean,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val background: Brush,
    val terminal: TerminalSkin,
)

private val Cousine = FontFamily(Font(R.font.cousine_regular))
private val PtSerif = FontFamily(
    Font(R.font.pt_serif_regular),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
)
private val PtSansNarrow = FontFamily(Font(R.font.pt_sans_narrow_regular))
private val Philosopher = FontFamily(
    Font(R.font.philosopher_regular),
    Font(R.font.philosopher_bold, FontWeight.Bold),
)
private val Pacifico = FontFamily(Font(R.font.pacifico_regular))
private val RubikMonoOne = FontFamily(Font(R.font.rubik_mono_one_regular))
private val YesevaOne = FontFamily(Font(R.font.yeseva_one_regular))
private val RuslanDisplay = FontFamily(Font(R.font.ruslan_display_regular))
private val PressStart = FontFamily(Font(R.font.press_start_2p_regular))

/** Раскладка оформления по стилю. */
fun AppStyle.spec(): StyleSpec = when (this) {
    AppStyle.NIGHT_TERMINAL -> StyleSpec(
        dark = true,
        colorScheme = buildScheme(
            dark = true,
            accent = Color(0xFF6BFF8E),
            onAccent = Color(0xFF00230C),
            accentContainer = Color(0xFF11341C),
            onAccentContainer = Color(0xFFB9FFCB),
            secondary = Color(0xFF8FE3B8),
            canvas = Color(0xFF070A08),
            ink = Color(0xFFD6F5DE),
            panel = Color(0xFF0E1511),
            subtleInk = Color(0xFF8FAF98),
            danger = Color(0xFFFF6E6E),
            outline = Color(0xFF2C4634),
        ),
        typography = buildTypography(display = Cousine, body = Cousine, displaySpacing = 1.sp),
        shapes = shapesOf(3),
        background = Brush.verticalGradient(listOf(Color(0xFF070A08), Color(0xFF0B1410))),
        terminal = TerminalSkin(
            background = Color(0xFF050806),
            foreground = Color(0xFFA8FFB8),
            palette = TerminalPalettes.PHOSPHOR,
        ),
    )

    AppStyle.PAPER_INK -> StyleSpec(
        dark = false,
        colorScheme = buildScheme(
            dark = false,
            accent = Color(0xFF9B3A22),
            onAccent = Color(0xFFFFF3E7),
            accentContainer = Color(0xFFF2DCC6),
            onAccentContainer = Color(0xFF4A1A0C),
            secondary = Color(0xFF6B5B45),
            canvas = Color(0xFFF6F0E1),
            ink = Color(0xFF221C13),
            panel = Color(0xFFFBF6EA),
            subtleInk = Color(0xFF6A5D49),
            danger = Color(0xFFA8231B),
            outline = Color(0xFFC7B79A),
        ),
        typography = buildTypography(
            display = YesevaOne,
            body = PtSerif,
            displayScale = 1.05f,
            bodyScale = 1.05f,
        ),
        shapes = shapesOf(10),
        background = Brush.verticalGradient(listOf(Color(0xFFF7F1E3), Color(0xFFEADFC6))),
        terminal = TerminalSkin(
            background = Color(0xFFFBF6EA),
            foreground = Color(0xFF2B2216),
            palette = TerminalPalettes.INK,
            fontFamily = Cousine,
        ),
    )

    AppStyle.SYNTHWAVE -> StyleSpec(
        dark = true,
        colorScheme = buildScheme(
            dark = true,
            accent = Color(0xFFFF4FA3),
            onAccent = Color(0xFF2A0018),
            accentContainer = Color(0xFF4A0F45),
            onAccentContainer = Color(0xFFFFD6EC),
            secondary = Color(0xFF35E7F5),
            canvas = Color(0xFF1B0B33),
            ink = Color(0xFFF6E7FF),
            panel = Color(0xFF2A1150),
            subtleInk = Color(0xFFBB9AE0),
            danger = Color(0xFFFF6B81),
            outline = Color(0xFF6C3BA0),
        ),
        typography = buildTypography(
            display = RubikMonoOne,
            body = PtSansNarrow,
            displaySpacing = 2.sp,
            bodySpacing = 0.4.sp,
            displayScale = 0.86f,
            bodyScale = 1.1f,
        ),
        shapes = shapesOf(18),
        background = Brush.linearGradient(
            listOf(Color(0xFF1B0B33), Color(0xFF3E1060), Color(0xFF7A1350)),
        ),
        terminal = TerminalSkin(
            background = Color(0xFF120726),
            foreground = Color(0xFFF7C8FF),
            palette = TerminalPalettes.NEON,
        ),
    )

    AppStyle.AURORA -> StyleSpec(
        dark = true,
        colorScheme = buildScheme(
            dark = true,
            accent = Color(0xFF57E8B4),
            onAccent = Color(0xFF002A20),
            accentContainer = Color(0xFF12463C),
            onAccentContainer = Color(0xFFC9FFEC),
            secondary = Color(0xFF9AB8FF),
            canvas = Color(0xFF06171C),
            ink = Color(0xFFE2F1EE),
            panel = Color(0xFF0E2A31),
            subtleInk = Color(0xFF9FBFBB),
            danger = Color(0xFFFF8A8A),
            outline = Color(0xFF2A5A60),
        ),
        typography = buildTypography(
            display = Philosopher,
            body = Philosopher,
            displaySpacing = 0.6.sp,
            displayScale = 1.12f,
            bodyScale = 1.08f,
        ),
        shapes = shapesOf(16),
        background = Brush.verticalGradient(
            listOf(Color(0xFF06171C), Color(0xFF0E3038), Color(0xFF113B31)),
        ),
        terminal = TerminalSkin(
            background = Color(0xFF041318),
            foreground = Color(0xFFD6F1EA),
            palette = TerminalPalettes.NEON,
        ),
    )

    AppStyle.COFFEE -> StyleSpec(
        dark = true,
        colorScheme = buildScheme(
            dark = true,
            accent = Color(0xFFD79A5B),
            onAccent = Color(0xFF2A1608),
            accentContainer = Color(0xFF4A3020),
            onAccentContainer = Color(0xFFFFE2C1),
            secondary = Color(0xFFB98A6A),
            canvas = Color(0xFF241812),
            ink = Color(0xFFF5E6D0),
            panel = Color(0xFF34231A),
            subtleInk = Color(0xFFC3A98F),
            danger = Color(0xFFE97B6B),
            outline = Color(0xFF5A4130),
        ),
        typography = buildTypography(
            display = Pacifico,
            body = PtSerif,
            displayScale = 1.18f,
            bodyScale = 1.05f,
        ),
        shapes = shapesOf(22),
        background = Brush.verticalGradient(listOf(Color(0xFF241812), Color(0xFF3A261A))),
        terminal = TerminalSkin(
            background = Color(0xFF1E1410),
            foreground = Color(0xFFFFC073),
            palette = TerminalPalettes.AMBER,
            fontFamily = Cousine,
        ),
    )

    AppStyle.ARCADE -> StyleSpec(
        dark = true,
        colorScheme = buildScheme(
            dark = true,
            accent = Color(0xFFFFE04B),
            onAccent = Color(0xFF241A00),
            accentContainer = Color(0xFF3A2A6B),
            onAccentContainer = Color(0xFFFFF3B8),
            secondary = Color(0xFFFF3355),
            canvas = Color(0xFF0B0B18),
            ink = Color(0xFFEDEBFF),
            panel = Color(0xFF1B1738),
            subtleInk = Color(0xFFA6A0D8),
            danger = Color(0xFFFF3355),
            outline = Color(0xFF4A3FA0),
        ),
        typography = buildTypography(
            display = PressStart,
            body = Cousine,
            displaySpacing = 0.sp,
            displayScale = 0.62f,
            bodyScale = 0.98f,
        ),
        shapes = shapesOf(0),
        background = Brush.verticalGradient(
            listOf(Color(0xFF0B0B18), Color(0xFF1A0F35), Color(0xFF2B0B3F)),
        ),
        terminal = TerminalSkin(
            background = Color(0xFF07060F),
            foreground = Color(0xFFFFE04B),
            palette = TerminalPalettes.NEON,
        ),
    )

    AppStyle.CHRONICLE -> StyleSpec(
        dark = false,
        colorScheme = buildScheme(
            dark = false,
            accent = Color(0xFF8A1F1F),
            onAccent = Color(0xFFF7ECD2),
            accentContainer = Color(0xFFE7D2A8),
            onAccentContainer = Color(0xFF3A1008),
            secondary = Color(0xFF6B5326),
            canvas = Color(0xFFEFE3C6),
            ink = Color(0xFF33240F),
            panel = Color(0xFFF6ECD6),
            subtleInk = Color(0xFF6E5A33),
            danger = Color(0xFF8A1F1F),
            outline = Color(0xFFBFA877),
        ),
        typography = buildTypography(
            display = RuslanDisplay,
            body = PtSerif,
            displaySpacing = 0.8.sp,
            displayScale = 1.08f,
            bodyScale = 1.06f,
        ),
        shapes = shapesOf(6),
        background = Brush.verticalGradient(listOf(Color(0xFFF0E4C6), Color(0xFFE0CFA6))),
        terminal = TerminalSkin(
            background = Color(0xFFF6ECD6),
            foreground = Color(0xFF3A2A15),
            palette = TerminalPalettes.INK,
            fontFamily = Cousine,
        ),
    )
}

private fun shapesOf(radius: Int): Shapes = Shapes(
    extraSmall = RoundedCornerShape((radius / 2).dp),
    small = RoundedCornerShape((radius * 3 / 4).dp),
    medium = RoundedCornerShape(radius.dp),
    large = RoundedCornerShape((radius * 3 / 2).dp),
    extraLarge = RoundedCornerShape((radius * 2).dp),
)

private fun TextUnit.scaled(factor: Float): TextUnit = if (isUnspecified) this else this * factor

private fun buildTypography(
    display: FontFamily,
    body: FontFamily,
    displaySpacing: TextUnit = 0.sp,
    bodySpacing: TextUnit = 0.sp,
    displayScale: Float = 1f,
    bodyScale: Float = 1f,
): Typography {
    val base = Typography()

    fun TextStyle.asDisplay(): TextStyle = copy(
        fontFamily = display,
        letterSpacing = displaySpacing,
        fontSize = fontSize.scaled(displayScale),
        lineHeight = lineHeight.scaled(displayScale),
    )

    fun TextStyle.asBody(): TextStyle = copy(
        fontFamily = body,
        letterSpacing = bodySpacing,
        fontSize = fontSize.scaled(bodyScale),
        lineHeight = lineHeight.scaled(bodyScale),
    )

    return base.copy(
        displayLarge = base.displayLarge.asDisplay(),
        displayMedium = base.displayMedium.asDisplay(),
        displaySmall = base.displaySmall.asDisplay(),
        headlineLarge = base.headlineLarge.asDisplay(),
        headlineMedium = base.headlineMedium.asDisplay(),
        headlineSmall = base.headlineSmall.asDisplay(),
        titleLarge = base.titleLarge.asDisplay(),
        titleMedium = base.titleMedium.asDisplay(),
        titleSmall = base.titleSmall.asDisplay(),
        bodyLarge = base.bodyLarge.asBody(),
        bodyMedium = base.bodyMedium.asBody(),
        bodySmall = base.bodySmall.asBody(),
        labelLarge = base.labelLarge.asBody(),
        labelMedium = base.labelMedium.asBody(),
        labelSmall = base.labelSmall.asBody(),
    )
}

/**
 * Собирает полную палитру Material 3 из десятка опорных цветов: роли контейнеров
 * выводятся подмешиванием, чтобы ни одна не осталась стандартной сиреневой.
 */
@Suppress("LongParameterList")
private fun buildScheme(
    dark: Boolean,
    accent: Color,
    onAccent: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    secondary: Color,
    canvas: Color,
    ink: Color,
    panel: Color,
    subtleInk: Color,
    danger: Color,
    outline: Color,
): ColorScheme {
    val step = if (dark) Color.White else Color.Black
    val tint = { fraction: Float -> lerp(panel, step, fraction) }
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        inversePrimary = accentContainer,
        secondary = secondary,
        onSecondary = onAccent,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        tertiary = secondary,
        onTertiary = onAccent,
        tertiaryContainer = accentContainer,
        onTertiaryContainer = onAccentContainer,
        background = canvas,
        onBackground = ink,
        surface = panel,
        onSurface = ink,
        surfaceVariant = tint(0.06f),
        onSurfaceVariant = subtleInk,
        surfaceTint = accent,
        inverseSurface = ink,
        inverseOnSurface = canvas,
        error = danger,
        onError = onAccent,
        errorContainer = lerp(danger, canvas, 0.72f),
        onErrorContainer = if (dark) Color.White else Color.White,
        outline = outline,
        outlineVariant = lerp(outline, panel, 0.5f),
        scrim = Color.Black,
        surfaceBright = tint(0.12f),
        surfaceDim = lerp(panel, if (dark) Color.Black else Color(0xFF9E9E9E), 0.35f),
        surfaceContainerLowest = tint(0.01f),
        surfaceContainerLow = tint(0.04f),
        surfaceContainer = tint(0.07f),
        surfaceContainerHigh = tint(0.10f),
        surfaceContainerHighest = tint(0.14f),
    )
}
