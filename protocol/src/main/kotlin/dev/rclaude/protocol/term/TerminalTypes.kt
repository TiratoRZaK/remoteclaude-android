package dev.rclaude.protocol.term

/** Цвет ячейки терминала. */
sealed interface TerminalColor {

    /** Цвет по умолчанию — фон или текст темы. */
    data object Default : TerminalColor

    /** Цвет из палитры терминала: 0–7 базовые, 8–15 яркие, 16–255 расширенные. */
    data class Indexed(val index: Int) : TerminalColor

    /** Цвет truecolor. */
    data class Rgb(val red: Int, val green: Int, val blue: Int) : TerminalColor
}

/** Оформление ячейки терминала. */
data class CellStyle(
    val foreground: TerminalColor = TerminalColor.Default,
    val background: TerminalColor = TerminalColor.Default,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val inverse: Boolean = false,
) {
    companion object {
        val DEFAULT: CellStyle = CellStyle()
    }
}

/** Кусок строки с единым оформлением. */
data class StyledRun(val text: String, val style: CellStyle)

/** Строка экрана: последовательность оформленных кусков без хвостовых пробелов. */
data class TerminalLine(val runs: List<StyledRun>) {

    /** Текст строки без оформления. */
    val text: String get() = runs.joinToString(separator = "") { it.text }

    companion object {
        val EMPTY: TerminalLine = TerminalLine(emptyList())
    }
}

/**
 * Снимок терминала для отрисовки: скроллбэк вместе с экраном, положение курсора в
 * координатах [lines] и номер ревизии (растёт с каждой порцией вывода).
 */
data class TerminalSnapshot(
    val lines: List<TerminalLine>,
    val cursorLine: Int,
    val cursorColumn: Int,
    val cols: Int,
    val rows: Int,
    val revision: Long,
) {
    companion object {
        val EMPTY: TerminalSnapshot = TerminalSnapshot(emptyList(), 0, 0, 80, 24, 0)
    }
}
