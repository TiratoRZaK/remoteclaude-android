package dev.rclaude.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalKeysTest {

    private val esc = Char(0x1B)

    @Test
    fun `управляющие последовательности совпадают с ожиданиями терминала`() {
        assertEquals(esc.toString(), TerminalKeys.ESC)
        assertEquals("$esc[Z", TerminalKeys.SHIFT_TAB)
        assertEquals("$esc[A", TerminalKeys.UP)
        assertEquals("$esc[B", TerminalKeys.DOWN)
        assertEquals("$esc[D", TerminalKeys.LEFT)
        assertEquals("$esc[C", TerminalKeys.RIGHT)
        assertEquals("\r", TerminalKeys.ENTER)
        assertEquals(Char(0x7F).toString(), TerminalKeys.BACKSPACE)
    }

    @Test
    fun `сочетания с Ctrl считаются от кода буквы`() {
        assertEquals(Char(0x03).toString(), TerminalKeys.CTRL_C)
        assertEquals(Char(0x12).toString(), TerminalKeys.CTRL_R)
        assertEquals(Char(0x14).toString(), TerminalKeys.CTRL_T)
        assertEquals(Char(0x02).toString(), TerminalKeys.CTRL_B)
        assertEquals("" + Char(0x05) + Char(0x15), TerminalKeys.CLEAR)
    }

    @Test
    fun `многострочный промпт уходит вставкой в скобках с переводом строки`() {
        val payload = TerminalKeys.bracketedPaste("первая\nвторая")

        assertEquals("$esc[200~первая\nвторая$esc[201~\r", payload)
    }

    @Test
    fun `вставка без отправки оставляет текст в строке ввода`() {
        val payload = TerminalKeys.bracketedPaste("из буфера", submit = false)

        assertEquals("$esc[200~из буфера$esc[201~", payload)
    }

    @Test
    fun `пустой промпт всё равно обёрнут корректно`() {
        assertEquals("$esc[200~$esc[201~\r", TerminalKeys.bracketedPaste(""))
    }
}
