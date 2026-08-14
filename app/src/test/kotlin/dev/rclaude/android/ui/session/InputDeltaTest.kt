package dev.rclaude.android.ui.session

import dev.rclaude.protocol.TerminalKeys
import kotlin.test.Test
import kotlin.test.assertEquals

class InputDeltaTest {

    private val del = TerminalKeys.BACKSPACE

    @Test
    fun `набранные символы уходят как есть`() {
        assertEquals("в", inputDelta("при", "прив"))
        assertEquals("привет", inputDelta("", "привет"))
    }

    @Test
    fun `удаление превращается в забой`() {
        assertEquals(del, inputDelta("прив", "при"))
        assertEquals(del + del + del, inputDelta("три", ""))
    }

    @Test
    fun `правка середины стирает хвост и печатает заново`() {
        assertEquals(del + del + "xc", inputDelta("abc", "axc"))
    }

    @Test
    fun `без изменений ничего не отправляется`() {
        assertEquals("", inputDelta("привет", "привет"))
        assertEquals("", inputDelta("", ""))
    }

    @Test
    fun `автозамена целого слова переписывает хвост`() {
        assertEquals(del.repeat(5) + "Привет", inputDelta("приве", "Привет"))
    }

    @Test
    fun `клавиши панели держат поле в согласии со строкой ввода`() {
        assertEquals("прив", "привет".afterKey(TerminalKeys.BACKSPACE).afterKey(TerminalKeys.BACKSPACE))
        assertEquals("", "привет".afterKey(TerminalKeys.ESC))
        assertEquals("", "привет".afterKey(TerminalKeys.CTRL_C))
        assertEquals("", "привет".afterKey(TerminalKeys.CLEAR))
        assertEquals("", "привет".afterKey(TerminalKeys.ENTER))
        assertEquals("привет", "привет".afterKey(TerminalKeys.UP))
    }
}
