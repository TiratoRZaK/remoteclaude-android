package dev.rclaude.android.ui.session

import androidx.compose.ui.graphics.Color
import dev.rclaude.protocol.term.CellStyle
import dev.rclaude.protocol.term.TerminalColor

/** Палитра терминала: 16 базовых цветов, куб 6×6×6 и серая шкала xterm. */
object TerminalPalette {

    /** Фон терминала — как у веб-клиента. */
    val Background: Color = Color(0xFF1A1A1A)

    /** Цвет текста по умолчанию. */
    val Foreground: Color = Color(0xFFE5E5E5)

    private val BASE = intArrayOf(
        0xFF2E3436.toInt(), 0xFFCD3131.toInt(), 0xFF0DBC79.toInt(), 0xFFE5E510.toInt(),
        0xFF2472C8.toInt(), 0xFFBC3FBC.toInt(), 0xFF11A8CD.toInt(), 0xFFE5E5E5.toInt(),
        0xFF666666.toInt(), 0xFFF14C4C.toInt(), 0xFF23D18B.toInt(), 0xFFF5F543.toInt(),
        0xFF3B8EEA.toInt(), 0xFFD670D6.toInt(), 0xFF29B8DB.toInt(), 0xFFFFFFFF.toInt(),
    )

    private val CUBE_STEPS = intArrayOf(0, 95, 135, 175, 215, 255)

    /** Цвет по индексу палитры 0–255. */
    fun indexed(index: Int): Color = when {
        index in 0..15 -> Color(BASE[index])
        index in 16..231 -> {
            val value = index - 16
            Color(CUBE_STEPS[value / 36], CUBE_STEPS[(value / 6) % 6], CUBE_STEPS[value % 6])
        }

        index in 232..255 -> {
            val level = 8 + (index - 232) * 10
            Color(level, level, level)
        }

        else -> Foreground
    }

    /** Цвета ячейки с учётом инверсии и тусклости. */
    fun resolve(style: CellStyle): Pair<Color, Color> {
        var foreground = color(style.foreground, Foreground)
        var background = color(style.background, Background)
        if (style.inverse) {
            val swapped = foreground
            foreground = background
            background = swapped
        }
        if (style.dim) foreground = foreground.copy(alpha = 0.65f)
        return foreground to background
    }

    private fun color(color: TerminalColor, fallback: Color): Color = when (color) {
        TerminalColor.Default -> fallback
        is TerminalColor.Indexed -> indexed(color.index)
        is TerminalColor.Rgb -> Color(color.red, color.green, color.blue)
    }
}
