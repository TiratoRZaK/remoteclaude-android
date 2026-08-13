package dev.rclaude.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import dev.rclaude.protocol.term.CellStyle
import dev.rclaude.protocol.term.TerminalColor

/**
 * Раскраска терминала: фон, цвет текста по умолчанию, палитра ANSI и моноширинный
 * шрифт. Меняется вместе с оформлением приложения.
 */
data class TerminalSkin(
    val background: Color,
    val foreground: Color,
    val palette: List<Color>,
    val fontFamily: FontFamily = FontFamily.Monospace,
) {

    private val lightCanvas: Boolean = background.luminance() > LIGHT_CANVAS_LUMINANCE

    /** Цвет по индексу палитры 0–255: базовые 16 из [palette], дальше куб и серая шкала. */
    fun indexed(index: Int): Color = adapt(
        when {
            index in palette.indices -> palette[index]
            index in 16..231 -> {
                val value = index - 16
                Color(CUBE[value / 36], CUBE[(value / 6) % 6], CUBE[value % 6])
            }

            index in 232..255 -> {
                val level = 8 + (index - 232) * 10
                Color(level, level, level)
            }

            else -> foreground
        },
    )

    /**
     * На светлом фоне бледные цвета из потока (серая шкала, пастель, truecolor)
     * притемняются, иначе вторичный текст Claude сливается с бумагой.
     */
    private fun adapt(color: Color): Color = when {
        !lightCanvas -> color
        color.luminance() > PALE_LUMINANCE -> lerp(color, Color.Black, PALE_DARKENING)
        else -> color
    }

    /** Цвета ячейки с учётом инверсии и тусклости. */
    fun resolve(style: CellStyle): Pair<Color, Color> {
        var text = color(style.foreground, foreground)
        var fill = color(style.background, background)
        if (style.inverse) {
            val swapped = text
            text = fill
            fill = swapped
        }
        if (style.dim) text = text.copy(alpha = 0.65f)
        return text to fill
    }

    private fun color(color: TerminalColor, fallback: Color): Color = when (color) {
        TerminalColor.Default -> fallback
        is TerminalColor.Indexed -> indexed(color.index)
        is TerminalColor.Rgb -> adapt(Color(color.red, color.green, color.blue))
    }

    private companion object {
        val CUBE = intArrayOf(0, 95, 135, 175, 215, 255)
        const val LIGHT_CANVAS_LUMINANCE = 0.5f
        const val PALE_LUMINANCE = 0.5f
        const val PALE_DARKENING = 0.62f
    }
}

/** Готовые палитры ANSI под разные оформления. */
object TerminalPalettes {

    /** Зелёный фосфор: акценты живые, но общий тон — свечение люминофора. */
    val PHOSPHOR: List<Color> = colors(
        0xFF0B140E, 0xFFFF6E6E, 0xFF66FF9E, 0xFFE6FF87, 0xFF6ED0FF, 0xFFD79BFF, 0xFF6FF2E0, 0xFFCFF5D8,
        0xFF3F5A48, 0xFFFF9494, 0xFF9BFFBE, 0xFFF2FFB0, 0xFF9CE0FF, 0xFFE7BCFF, 0xFF9FFCEE, 0xFFF2FFF6,
    )

    /** Неон: насыщенные цвета для тёмных градиентов. */
    val NEON: List<Color> = colors(
        0xFF150C22, 0xFFFF5C7A, 0xFF3BF0A5, 0xFFFFE066, 0xFF5AC8FF, 0xFFFF6FE0, 0xFF52E7F0, 0xFFEDE4FF,
        0xFF5A4E7A, 0xFFFF89A0, 0xFF7DFFC4, 0xFFFFF0A3, 0xFF8FDCFF, 0xFFFFA3EE, 0xFF9BF4F8, 0xFFFFFFFF,
    )

    /** Чернила по бумаге: тёмные цвета, читаемые на светлом фоне. */
    val INK: List<Color> = colors(
        0xFFE8DFC7, 0xFF9B2B1F, 0xFF3F6B32, 0xFF8A6A12, 0xFF2C4E86, 0xFF7A2E75, 0xFF1F6A72, 0xFF3A2F20,
        0xFFB6A88A, 0xFFB8402F, 0xFF4F8440, 0xFFA5811C, 0xFF3A62A6, 0xFF95398F, 0xFF2A838C, 0xFF221A10,
    )

    /** Янтарный монитор: тёплая гамма без синевы. */
    val AMBER: List<Color> = colors(
        0xFF1A120C, 0xFFFF7A55, 0xFFE0B24A, 0xFFFFD07A, 0xFFC98A4B, 0xFFFF9E6B, 0xFFE8C489, 0xFFFFE0B2,
        0xFF5A4130, 0xFFFF9B7A, 0xFFF0C868, 0xFFFFE0A0, 0xFFE0A566, 0xFFFFBC93, 0xFFF5D8AC, 0xFFFFF3E0,
    )

    private fun colors(vararg values: Long): List<Color> = values.map { Color(it) }
}
