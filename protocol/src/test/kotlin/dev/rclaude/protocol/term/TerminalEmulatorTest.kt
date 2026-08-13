package dev.rclaude.protocol.term

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalEmulatorTest {

    private val esc = Char(0x1B)
    private val bell = Char(0x07)

    private fun TerminalEmulator.textLines(): List<String> = snapshot().lines.map { it.text }

    @Test
    fun `печатает текст и держит курсор`() {
        val terminal = TerminalEmulator(cols = 20, rows = 5)

        terminal.writeText("привет")

        assertEquals(listOf("привет"), terminal.textLines())
        assertEquals(6, terminal.snapshot().cursorColumn)
        assertEquals(0, terminal.snapshot().cursorLine)
    }

    @Test
    fun `переносит строку по ширине экрана`() {
        val terminal = TerminalEmulator(cols = 5, rows = 4)

        terminal.writeText("abcdefg")

        assertEquals(listOf("abcde", "fg"), terminal.textLines())
    }

    @Test
    fun `возврат каретки переписывает строку`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)

        terminal.writeText("hello\rH")

        assertEquals(listOf("Hello"), terminal.textLines())
    }

    @Test
    fun `забой сдвигает курсор назад`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)

        terminal.writeText("abc\b\bX")

        assertEquals(listOf("aXc"), terminal.textLines())
    }

    @Test
    fun `позиционирование курсора пишет в нужную строку`() {
        val terminal = TerminalEmulator(cols = 10, rows = 3)

        terminal.writeText("$esc[2;3Hxy")

        assertEquals(listOf("", "  xy"), terminal.textLines())
    }

    @Test
    fun `очистка экрана убирает содержимое`() {
        val terminal = TerminalEmulator(cols = 10, rows = 3)
        terminal.writeText("abc\r\ndef")

        terminal.writeText("$esc[2J")

        assertEquals(listOf("", ""), terminal.textLines())
    }

    @Test
    fun `очистка строки от курсора обрезает хвост`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)
        terminal.writeText("abcdef")

        terminal.writeText("$esc[1;4H$esc[0K")

        assertEquals(listOf("abc"), terminal.textLines())
    }

    @Test
    fun `цвет SGR раскладывается по кускам строки`() {
        val terminal = TerminalEmulator(cols = 20, rows = 2)

        terminal.writeText("$esc[31mred$esc[0m plain")

        val runs = terminal.snapshot().lines[0].runs
        assertEquals(2, runs.size)
        assertEquals("red", runs[0].text)
        assertEquals(TerminalColor.Indexed(1), runs[0].style.foreground)
        assertEquals(" plain", runs[1].text)
        assertEquals(CellStyle.DEFAULT, runs[1].style)
    }

    @Test
    fun `атрибуты и яркие цвета`() {
        val terminal = TerminalEmulator(cols = 20, rows = 2)

        terminal.writeText("$esc[1;4;92mbright")

        val style = terminal.snapshot().lines[0].runs[0].style
        assertTrue(style.bold)
        assertTrue(style.underline)
        assertEquals(TerminalColor.Indexed(10), style.foreground)
    }

    @Test
    fun `палитра из 256 цветов и truecolor`() {
        val terminal = TerminalEmulator(cols = 30, rows = 2)

        terminal.writeText("$esc[38;5;196mA$esc[48;2;10;20;30mB")

        val runs = terminal.snapshot().lines[0].runs
        assertEquals(TerminalColor.Indexed(196), runs[0].style.foreground)
        assertEquals(TerminalColor.Rgb(10, 20, 30), runs[1].style.background)
    }

    @Test
    fun `truecolor с двоеточиями`() {
        val terminal = TerminalEmulator(cols = 30, rows = 2)

        terminal.writeText("$esc[38:2::1:2:3mX")

        assertEquals(TerminalColor.Rgb(1, 2, 3), terminal.snapshot().lines[0].runs[0].style.foreground)
    }

    @Test
    fun `ушедшие вверх строки попадают в скроллбэк`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)

        terminal.writeText("one\r\ntwo\r\nthree")

        assertEquals(listOf("one", "two", "three"), terminal.textLines())
        assertEquals(2, terminal.snapshot().cursorLine)
    }

    @Test
    fun `скроллбэк ограничен по размеру`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2, scrollbackLimit = 2)

        terminal.writeText("1\r\n2\r\n3\r\n4\r\n5")

        assertEquals(listOf("2", "3", "4", "5"), terminal.textLines())
    }

    @Test
    fun `последовательность, разрезанная между кадрами, собирается`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)

        terminal.writeText("$esc[3")
        terminal.writeText("1mR")

        assertEquals(TerminalColor.Indexed(1), terminal.snapshot().lines[0].runs[0].style.foreground)
        assertEquals(listOf("R"), terminal.textLines())
    }

    @Test
    fun `разрезанный многобайтовый символ собирается из двух кадров`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)
        val bytes = "привет".toByteArray(Charsets.UTF_8)

        terminal.write(bytes.copyOfRange(0, 5))
        terminal.write(bytes.copyOfRange(5, bytes.size))

        assertEquals(listOf("привет"), terminal.textLines())
    }

    @Test
    fun `альтернативный экран не трогает основной`() {
        val terminal = TerminalEmulator(cols = 10, rows = 3)
        terminal.writeText("main")

        terminal.writeText("$esc[?1049h")
        terminal.writeText("alt")

        assertTrue(terminal.alternateScreen)
        assertEquals(listOf("alt"), terminal.textLines())

        terminal.writeText("$esc[?1049l")

        assertFalse(terminal.alternateScreen)
        assertEquals(listOf("main"), terminal.textLines())
    }

    @Test
    fun `вставка и удаление строк`() {
        val terminal = TerminalEmulator(cols = 5, rows = 3)
        terminal.writeText("a\r\nb\r\nc")

        terminal.writeText("$esc[1;1H$esc[L")

        assertEquals(listOf("", "a", "b"), terminal.textLines())

        terminal.writeText("$esc[1;1H$esc[2M")

        assertEquals(listOf("b"), terminal.textLines())
    }

    @Test
    fun `вставка, удаление и стирание символов`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)
        terminal.writeText("abcdef")

        terminal.writeText("$esc[1;3H$esc[2P")
        assertEquals(listOf("abef"), terminal.textLines())

        terminal.writeText("$esc[1;3H$esc[2@")
        assertEquals(listOf("ab  ef"), terminal.textLines())

        terminal.writeText("$esc[1;1H$esc[2X")
        assertEquals(listOf("    ef"), terminal.textLines())
    }

    @Test
    fun `область прокрутки двигает только свои строки`() {
        val terminal = TerminalEmulator(cols = 5, rows = 4)

        terminal.writeText("$esc[2;3r$esc[2;1Ha\r\nb\r\nc")

        assertEquals(listOf("", "b", "c"), terminal.textLines())
        assertTrue(terminal.snapshot().lines.size == 3)
    }

    @Test
    fun `заголовок окна и прочие OSC проглатываются`() {
        val terminal = TerminalEmulator(cols = 20, rows = 2)

        terminal.writeText("$esc]0;Заголовок окна${bell}text")

        assertEquals(listOf("text"), terminal.textLines())
    }

    @Test
    fun `запросы состояния не печатаются как текст`() {
        val terminal = TerminalEmulator(cols = 20, rows = 2)

        terminal.writeText("$esc[?25l$esc[6n$esc[?2004hgo")

        assertEquals(listOf("go"), terminal.textLines())
    }

    @Test
    fun `изменение размера сохраняет содержимое`() {
        val terminal = TerminalEmulator(cols = 10, rows = 3)
        terminal.writeText("hello\r\nworld")

        terminal.resize(3, 2)

        assertEquals(3, terminal.cols)
        assertEquals(2, terminal.rows)
        assertEquals(listOf("hel", "wor"), terminal.textLines())
    }

    @Test
    fun `полная очистка убирает экран и скроллбэк`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)
        terminal.writeText("1\r\n2\r\n3")

        terminal.reset()

        assertEquals(listOf(""), terminal.textLines())
        assertEquals(0, terminal.snapshot().cursorLine)
    }

    @Test
    fun `перерисовка строки поверх себя не плодит копий`() {
        val terminal = TerminalEmulator(cols = 20, rows = 3)

        terminal.writeText("Пишу ответ…")
        terminal.writeText("\r$esc[KГотово")

        assertEquals(listOf("Готово"), terminal.textLines())
    }

    @Test
    fun `ревизия растёт с каждой порцией вывода`() {
        val terminal = TerminalEmulator(cols = 10, rows = 2)
        val before = terminal.snapshot().revision

        terminal.writeText("x")

        assertTrue(terminal.snapshot().revision > before)
    }
}
