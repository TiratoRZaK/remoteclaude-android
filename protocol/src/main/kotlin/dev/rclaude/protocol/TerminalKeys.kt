package dev.rclaude.protocol

/**
 * Escape-последовательности панели быстрых клавиш и вставки промпта.
 *
 * Управляющие символы собираются из кодов ([Char] от кода), а не пишутся в исходнике
 * невидимыми литералами.
 */
object TerminalKeys {

    /** Кнопка панели: подпись и то, что уходит в PTY. */
    data class Key(val label: String, val sequence: String)

    /** Начало управляющей последовательности CSI: ESC + `[`. */
    val CSI: String = Char(0x1B) + "["

    val ESC: String = Char(0x1B).toString()
    val SHIFT_TAB: String = CSI + "Z"
    val UP: String = CSI + "A"
    val DOWN: String = CSI + "B"
    val LEFT: String = CSI + "D"
    val RIGHT: String = CSI + "C"
    val ENTER: String = "\r"
    val CTRL_C: String = Char(0x03).toString()

    /** Ctrl+E (в конец строки) + Ctrl+U (стереть до начала) — чистит ввод целиком. */
    val CLEAR: String = "" + Char(0x05) + Char(0x15)

    private val PASTE_START: String = CSI + "200~"
    private val PASTE_END: String = CSI + "201~"

    /** Панель быстрых клавиш в порядке показа. */
    val QUICK_KEYS: List<Key> = listOf(
        Key("Esc", ESC),
        Key("⇧Tab", SHIFT_TAB),
        Key("↑", UP),
        Key("↓", DOWN),
        Key("←", LEFT),
        Key("→", RIGHT),
        Key("Enter", ENTER),
        Key("Ctrl+C", CTRL_C),
        Key("Clear", CLEAR),
        Key("1", "1"),
        Key("2", "2"),
        Key("3", "3"),
        Key("4", "4"),
        Key("5", "5"),
    )

    /**
     * Оборачивает промпт в bracketed paste и добавляет Enter: многострочный текст
     * попадает в Claude одной вставкой и сразу отправляется.
     */
    fun bracketedPaste(text: String): String = PASTE_START + text + PASTE_END + ENTER
}
