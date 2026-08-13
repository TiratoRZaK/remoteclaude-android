package dev.rclaude.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalKeysTest {

    private val esc = Char(0x1B)

    @Test
    fun `последовательности клавиш совпадают с веб-клиентом`() {
        assertEquals(esc.toString(), TerminalKeys.ESC)
        assertEquals("$esc[Z", TerminalKeys.SHIFT_TAB)
        assertEquals("$esc[A", TerminalKeys.UP)
        assertEquals("$esc[B", TerminalKeys.DOWN)
        assertEquals("$esc[D", TerminalKeys.LEFT)
        assertEquals("$esc[C", TerminalKeys.RIGHT)
        assertEquals("\r", TerminalKeys.ENTER)
        assertEquals(Char(0x03).toString(), TerminalKeys.CTRL_C)
        assertEquals("" + Char(0x05) + Char(0x15), TerminalKeys.CLEAR)
    }

    @Test
    fun `панель повторяет состав веб-клиента`() {
        val labels = TerminalKeys.QUICK_KEYS.map { it.label }

        assertEquals(
            listOf("Esc", "⇧Tab", "↑", "↓", "←", "→", "Enter", "Ctrl+C", "Clear", "1", "2", "3", "4", "5"),
            labels,
        )
        assertTrue(TerminalKeys.QUICK_KEYS.all { it.sequence.isNotEmpty() })
    }

    @Test
    fun `многострочный промпт уходит вставкой в скобках с переводом строки`() {
        val payload = TerminalKeys.bracketedPaste("первая\nвторая")

        assertEquals("$esc[200~первая\nвторая$esc[201~\r", payload)
    }

    @Test
    fun `пустой промпт всё равно обёрнут корректно`() {
        assertEquals("$esc[200~$esc[201~\r", TerminalKeys.bracketedPaste(""))
    }
}
