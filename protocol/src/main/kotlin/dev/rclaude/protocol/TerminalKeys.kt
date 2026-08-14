package dev.rclaude.protocol

/**
 * Escape-последовательности клавиш и вставки промпта.
 *
 * Управляющие символы собираются из кодов ([Char] от кода), а не пишутся в исходнике
 * невидимыми литералами.
 */
object TerminalKeys {

    /** Начало управляющей последовательности CSI: ESC + `[`. */
    val CSI: String = Char(0x1B) + "["

    val ESC: String = Char(0x1B).toString()
    val SHIFT_TAB: String = CSI + "Z"
    val UP: String = CSI + "A"
    val DOWN: String = CSI + "B"
    val LEFT: String = CSI + "D"
    val RIGHT: String = CSI + "C"
    val ENTER: String = "\r"

    /** Забой: терминалы посылают DEL, его же ждёт строка ввода Claude Code. */
    val BACKSPACE: String = Char(0x7F).toString()

    /** Ctrl+C — прервать текущую работу. */
    val CTRL_C: String = Char(0x03).toString()

    /** Ctrl+R — служебное сочетание Claude Code (подробности прогона). */
    val CTRL_R: String = Char(0x12).toString()

    /** Ctrl+T — показать или скрыть список задач. */
    val CTRL_T: String = Char(0x14).toString()

    /** Ctrl+B — увести текущую работу в фон. */
    val CTRL_B: String = Char(0x02).toString()

    /** Ctrl+E (в конец строки) + Ctrl+U (стереть до начала) — чистит ввод целиком. */
    val CLEAR: String = "" + Char(0x05) + Char(0x15)

    private val PASTE_START: String = CSI + "200~"
    private val PASTE_END: String = CSI + "201~"

    /**
     * Оборачивает текст в bracketed paste: многострочный промпт попадает в Claude
     * одной вставкой. При [submit] в конце добавляется Enter — вставка сразу
     * отправляется, иначе текст остаётся в строке ввода для правки.
     */
    fun bracketedPaste(text: String, submit: Boolean = true): String =
        PASTE_START + text + PASTE_END + if (submit) ENTER else ""
}
