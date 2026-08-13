package dev.rclaude.protocol.term

/**
 * Экранный буфер терминала: разбирает поток PTY (UTF-8 + ANSI) в сетку ячеек
 * `cols × rows` и скроллбэк готовых строк.
 *
 * Это читающая реализация, а не полный VT220: поддержаны перемещение курсора,
 * очистки, вставка и удаление строк и символов, область прокрутки, оформление SGR
 * (включая 256 цветов и truecolor), альтернативный экран и автоперенос. Мышь,
 * графика, двойная ширина и ответы на запросы состояния не поддерживаются —
 * такие последовательности проглатываются.
 *
 * Класс не потокобезопасен: работайте с ним из одной корутины.
 */
class TerminalEmulator(
    cols: Int = 80,
    rows: Int = 24,
    private val scrollbackLimit: Int = 1_500,
) {

    /** Ширина экрана в символах. */
    var cols: Int = cols.coerceAtLeast(1)
        private set

    /** Высота экрана в строках. */
    var rows: Int = rows.coerceAtLeast(1)
        private set

    private val decoder = Utf8StreamDecoder()
    private val scrollback = ArrayDeque<TerminalLine>()
    private var screen: MutableList<Row> = blankRows(this.cols, this.rows)
    private var storedMainScreen: MutableList<Row>? = null
    private var storedMainCursor: Cursor? = null
    private var savedCursor: Cursor? = null

    private var row = 0
    private var column = 0
    private var style = CellStyle.DEFAULT
    private var scrollTop = 0
    private var scrollBottom = this.rows - 1
    private var pendingWrap = false
    private var autoWrap = true
    private var revision = 0L

    private var state = State.GROUND
    private val sequence = StringBuilder()

    /** Идёт ли работа в альтернативном экране (полноэкранная программа). */
    val alternateScreen: Boolean get() = storedMainScreen != null

    /** Принимает порцию байтов вывода PTY. */
    fun write(bytes: ByteArray) {
        writeText(decoder.decode(bytes))
    }

    /** Принимает уже декодированный текст — удобно для тестов и вставок. */
    fun writeText(text: String) {
        for (char in text) feed(char)
        revision++
    }

    /** Снимок для отрисовки: скроллбэк, экран и положение курсора. */
    fun snapshot(): TerminalSnapshot {
        val screenLines = screen.map { it.toLine() }
        val lastFilled = screenLines.indexOfLast { it.runs.isNotEmpty() }
        val keep = maxOf(lastFilled + 1, row + 1).coerceIn(1, screenLines.size)
        val lines = ArrayList<TerminalLine>(scrollback.size + keep)
        lines.addAll(scrollback)
        for (index in 0 until keep) lines.add(screenLines[index])
        return TerminalSnapshot(
            lines = lines,
            cursorLine = scrollback.size + row,
            cursorColumn = column,
            cols = cols,
            rows = rows,
            revision = revision,
        )
    }

    /** Меняет геометрию экрана, сохраняя содержимое. */
    fun resize(newCols: Int, newRows: Int) {
        val targetCols = newCols.coerceAtLeast(1)
        val targetRows = newRows.coerceAtLeast(1)
        if (targetCols == cols && targetRows == rows) return
        val resized = screen.map { it.resized(targetCols) }.toMutableList()
        while (resized.size > targetRows) {
            val dropped = resized.removeAt(0)
            if (!alternateScreen) pushToScrollback(dropped)
            row--
        }
        while (resized.size < targetRows) resized.add(Row(targetCols))
        screen = resized
        storedMainScreen = storedMainScreen?.let { stored ->
            val normalized = stored.map { it.resized(targetCols) }.toMutableList()
            while (normalized.size > targetRows) normalized.removeAt(0)
            while (normalized.size < targetRows) normalized.add(Row(targetCols))
            normalized
        }
        cols = targetCols
        rows = targetRows
        scrollTop = 0
        scrollBottom = rows - 1
        row = row.coerceIn(0, rows - 1)
        column = column.coerceIn(0, cols - 1)
        pendingWrap = false
        revision++
    }

    /** Полная очистка: экран, скроллбэк, оформление и состояние разбора. */
    fun reset() {
        decoder.reset()
        scrollback.clear()
        screen = blankRows(cols, rows)
        storedMainScreen = null
        storedMainCursor = null
        savedCursor = null
        row = 0
        column = 0
        style = CellStyle.DEFAULT
        scrollTop = 0
        scrollBottom = rows - 1
        pendingWrap = false
        autoWrap = true
        state = State.GROUND
        sequence.setLength(0)
        revision++
    }

    private fun feed(char: Char) {
        when (state) {
            State.GROUND -> ground(char)
            State.ESCAPE -> escape(char)
            State.CSI -> csi(char)
            State.OSC -> stringSequence(char, State.OSC, State.OSC_ESCAPE)
            State.OSC_ESCAPE -> state = if (char == '\\') State.GROUND else State.OSC
            State.STRING -> stringSequence(char, State.STRING, State.STRING_ESCAPE)
            State.STRING_ESCAPE -> state = if (char == '\\') State.GROUND else State.STRING
            State.CONSUME_ONE -> state = State.GROUND
        }
    }

    private fun ground(char: Char) {
        when (char.code) {
            ESCAPE_CODE -> state = State.ESCAPE
            CARRIAGE_RETURN -> {
                column = 0
                pendingWrap = false
            }

            LINE_FEED, VERTICAL_TAB, FORM_FEED -> lineFeed()
            BACKSPACE -> if (pendingWrap) pendingWrap = false else if (column > 0) column--
            TAB -> tabForward(1)
            BELL -> Unit
            else -> if (char.code >= 0x20 && char.code != DELETE) putChar(char)
        }
    }

    private fun escape(char: Char) {
        state = State.GROUND
        when (char) {
            '[' -> {
                sequence.setLength(0)
                state = State.CSI
            }

            ']' -> state = State.OSC
            'P', '_', '^', 'X' -> state = State.STRING
            '(', ')', '*', '+', '-', '.', '/', '#', ' ' -> state = State.CONSUME_ONE
            '7' -> savedCursor = Cursor(row, column, style)
            '8' -> restoreCursor()
            'M' -> reverseIndex()
            'D' -> lineFeed()
            'E' -> {
                column = 0
                lineFeed()
            }

            'c' -> reset()
            else -> Unit
        }
    }

    private fun csi(char: Char) {
        when {
            char.code == ESCAPE_CODE -> state = State.ESCAPE
            char.code in 0x40..0x7E -> {
                state = State.GROUND
                execute(char)
            }

            char.code in 0x20..0x3F -> sequence.append(char)
            else -> Unit
        }
    }

    private fun stringSequence(char: Char, body: State, escapeState: State) {
        state = when (char.code) {
            BELL -> State.GROUND
            ESCAPE_CODE -> escapeState
            else -> body
        }
    }

    private fun execute(final: Char) {
        val body = sequence.toString()
        sequence.setLength(0)
        val marker = body.firstOrNull()?.takeIf { it in "?<=>" }
        val params = (if (marker != null) body.substring(1) else body)
            .filter { it.isDigit() || it == ';' || it == ':' }
            .split(';')

        fun param(index: Int, fallback: Int): Int =
            params.getOrNull(index)?.substringBefore(':')?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: fallback

        when (final) {
            'A' -> moveUp(param(0, 1))
            'B' -> moveDown(param(0, 1))
            'C' -> moveRight(param(0, 1))
            'D' -> moveLeft(param(0, 1))
            'E' -> {
                moveDown(param(0, 1))
                column = 0
            }

            'F' -> {
                moveUp(param(0, 1))
                column = 0
            }

            'G', '`' -> setColumn(param(0, 1) - 1)
            'd' -> setRow(param(0, 1) - 1)
            'H', 'f' -> {
                setRow(param(0, 1) - 1)
                setColumn(param(1, 1) - 1)
            }

            'I' -> tabForward(param(0, 1))
            'Z' -> tabBackward(param(0, 1))
            'J' -> eraseInDisplay(param(0, 0))
            'K' -> eraseInLine(param(0, 0))
            'L' -> insertLines(param(0, 1))
            'M' -> deleteLines(param(0, 1))
            '@' -> insertCharacters(param(0, 1))
            'P' -> deleteCharacters(param(0, 1))
            'X' -> eraseCharacters(param(0, 1))
            'S' -> scrollUp(param(0, 1))
            'T' -> scrollDown(param(0, 1))
            'r' -> setScrollRegion(param(0, 1), param(1, rows))
            'm' -> applyGraphicRendition(params)
            'h' -> setPrivateModes(marker, params, enabled = true)
            'l' -> setPrivateModes(marker, params, enabled = false)
            's' -> if (marker == null) savedCursor = Cursor(row, column, style)
            'u' -> if (marker == null) restoreCursor()
            else -> Unit
        }
    }

    private fun putChar(char: Char) {
        if (pendingWrap) {
            if (autoWrap) {
                column = 0
                lineFeed()
            }
            pendingWrap = false
        }
        screen[row].set(column, char, style)
        if (column >= cols - 1) pendingWrap = true else column++
    }

    private fun lineFeed() {
        pendingWrap = false
        when {
            row == scrollBottom -> scrollUp(1)
            row < rows - 1 -> row++
        }
    }

    private fun reverseIndex() {
        pendingWrap = false
        when {
            row == scrollTop -> scrollDown(1)
            row > 0 -> row--
        }
    }

    private fun restoreCursor() {
        val saved = savedCursor ?: return
        row = saved.row.coerceIn(0, rows - 1)
        column = saved.column.coerceIn(0, cols - 1)
        style = saved.style
        pendingWrap = false
    }

    private fun moveUp(count: Int) {
        val limit = if (row >= scrollTop) scrollTop else 0
        row = maxOf(row - count, limit)
        pendingWrap = false
    }

    private fun moveDown(count: Int) {
        val limit = if (row <= scrollBottom) scrollBottom else rows - 1
        row = minOf(row + count, limit)
        pendingWrap = false
    }

    private fun moveLeft(count: Int) {
        column = maxOf(column - count, 0)
        pendingWrap = false
    }

    private fun moveRight(count: Int) {
        column = minOf(column + count, cols - 1)
        pendingWrap = false
    }

    private fun setRow(value: Int) {
        row = value.coerceIn(0, rows - 1)
        pendingWrap = false
    }

    private fun setColumn(value: Int) {
        column = value.coerceIn(0, cols - 1)
        pendingWrap = false
    }

    private fun tabForward(count: Int) {
        repeat(count) {
            val next = ((column / TAB_WIDTH) + 1) * TAB_WIDTH
            column = minOf(next, cols - 1)
        }
        pendingWrap = false
    }

    private fun tabBackward(count: Int) {
        repeat(count) {
            val previous = ((column - 1) / TAB_WIDTH) * TAB_WIDTH
            column = maxOf(previous, 0)
        }
        pendingWrap = false
    }

    private fun eraseInDisplay(mode: Int) {
        val blank = eraseStyle()
        when (mode) {
            0 -> {
                screen[row].clear(column, cols - 1, blank)
                for (index in row + 1 until rows) screen[index].clear(0, cols - 1, blank)
            }

            1 -> {
                for (index in 0 until row) screen[index].clear(0, cols - 1, blank)
                screen[row].clear(0, column, blank)
            }

            2 -> for (index in 0 until rows) screen[index].clear(0, cols - 1, blank)
            3 -> scrollback.clear()
            else -> Unit
        }
        pendingWrap = false
    }

    private fun eraseInLine(mode: Int) {
        val blank = eraseStyle()
        when (mode) {
            0 -> screen[row].clear(column, cols - 1, blank)
            1 -> screen[row].clear(0, column, blank)
            2 -> screen[row].clear(0, cols - 1, blank)
            else -> Unit
        }
        pendingWrap = false
    }

    private fun insertLines(count: Int) {
        if (row < scrollTop || row > scrollBottom) return
        repeat(minOf(count, scrollBottom - row + 1)) {
            screen.removeAt(scrollBottom)
            screen.add(row, Row(cols))
        }
        column = 0
        pendingWrap = false
    }

    private fun deleteLines(count: Int) {
        if (row < scrollTop || row > scrollBottom) return
        repeat(minOf(count, scrollBottom - row + 1)) {
            screen.removeAt(row)
            screen.add(scrollBottom, Row(cols))
        }
        column = 0
        pendingWrap = false
    }

    private fun insertCharacters(count: Int) {
        screen[row].insert(column, count, eraseStyle())
        pendingWrap = false
    }

    private fun deleteCharacters(count: Int) {
        screen[row].delete(column, count, eraseStyle())
        pendingWrap = false
    }

    private fun eraseCharacters(count: Int) {
        val last = minOf(column + count - 1, cols - 1)
        screen[row].clear(column, last, eraseStyle())
        pendingWrap = false
    }

    private fun scrollUp(count: Int) {
        repeat(minOf(count, rows)) {
            val dropped = screen.removeAt(scrollTop)
            if (scrollTop == 0 && !alternateScreen) pushToScrollback(dropped)
            screen.add(scrollBottom, Row(cols))
        }
        pendingWrap = false
    }

    private fun scrollDown(count: Int) {
        repeat(minOf(count, rows)) {
            screen.removeAt(scrollBottom)
            screen.add(scrollTop, Row(cols))
        }
        pendingWrap = false
    }

    private fun setScrollRegion(top: Int, bottom: Int) {
        val newTop = (top - 1).coerceIn(0, rows - 1)
        val newBottom = (bottom - 1).coerceIn(0, rows - 1)
        if (newTop < newBottom) {
            scrollTop = newTop
            scrollBottom = newBottom
        } else {
            scrollTop = 0
            scrollBottom = rows - 1
        }
        row = 0
        column = 0
        pendingWrap = false
    }

    private fun applyGraphicRendition(params: List<String>) {
        if (params.all { it.isBlank() }) {
            style = CellStyle.DEFAULT
            return
        }
        var index = 0
        while (index < params.size) {
            val raw = params[index]
            if (raw.contains(':')) {
                applySubParameters(raw.split(':'))
                index++
                continue
            }
            when (val code = raw.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0) {
                0 -> style = CellStyle.DEFAULT
                1 -> style = style.copy(bold = true)
                2 -> style = style.copy(dim = true)
                3 -> style = style.copy(italic = true)
                4 -> style = style.copy(underline = true)
                7 -> style = style.copy(inverse = true)
                21, 22 -> style = style.copy(bold = false, dim = false)
                23 -> style = style.copy(italic = false)
                24 -> style = style.copy(underline = false)
                27 -> style = style.copy(inverse = false)
                in 30..37 -> style = style.copy(foreground = TerminalColor.Indexed(code - 30))
                39 -> style = style.copy(foreground = TerminalColor.Default)
                in 40..47 -> style = style.copy(background = TerminalColor.Indexed(code - 40))
                49 -> style = style.copy(background = TerminalColor.Default)
                in 90..97 -> style = style.copy(foreground = TerminalColor.Indexed(code - 90 + 8))
                in 100..107 -> style = style.copy(background = TerminalColor.Indexed(code - 100 + 8))
                38, 48 -> {
                    val extended = readExtendedColor(params, index)
                    if (extended.color != null) {
                        style = if (code == 38) {
                            style.copy(foreground = extended.color)
                        } else {
                            style.copy(background = extended.color)
                        }
                    }
                    index += extended.consumed
                }

                else -> Unit
            }
            index++
        }
    }

    private fun applySubParameters(parts: List<String>) {
        val numbers = parts.map { it.toIntOrNull() }
        when (numbers.firstOrNull()) {
            38, 48 -> {
                val color = when (numbers.getOrNull(1)) {
                    5 -> numbers.getOrNull(2)?.let { TerminalColor.Indexed(it) }
                    2 -> numbers.drop(2).filterNotNull().takeIf { it.size >= 3 }?.let {
                        TerminalColor.Rgb(it[it.size - 3], it[it.size - 2], it[it.size - 1])
                    }

                    else -> null
                }
                if (color != null) {
                    style = if (numbers.first() == 38) {
                        style.copy(foreground = color)
                    } else {
                        style.copy(background = color)
                    }
                }
            }

            4 -> style = style.copy(underline = numbers.getOrNull(1) != 0)
            else -> Unit
        }
    }

    private fun readExtendedColor(params: List<String>, index: Int): ExtendedColor {
        fun number(offset: Int): Int? = params.getOrNull(index + offset)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        return when (number(1)) {
            5 -> ExtendedColor(number(2)?.let { TerminalColor.Indexed(it) }, consumed = 2)
            2 -> {
                val red = number(2)
                val green = number(3)
                val blue = number(4)
                val color = if (red != null && green != null && blue != null) {
                    TerminalColor.Rgb(red, green, blue)
                } else {
                    null
                }
                ExtendedColor(color, consumed = 4)
            }

            else -> ExtendedColor(null, consumed = 0)
        }
    }

    private fun setPrivateModes(marker: Char?, params: List<String>, enabled: Boolean) {
        if (marker != '?') return
        for (raw in params) {
            when (raw.takeIf { it.isNotBlank() }?.toIntOrNull()) {
                7 -> autoWrap = enabled
                47, 1047, 1049 -> if (enabled) enterAlternateScreen() else leaveAlternateScreen()
                else -> Unit
            }
        }
    }

    private fun enterAlternateScreen() {
        if (alternateScreen) return
        storedMainScreen = screen
        storedMainCursor = Cursor(row, column, style)
        screen = blankRows(cols, rows)
        row = 0
        column = 0
        pendingWrap = false
    }

    private fun leaveAlternateScreen() {
        val stored = storedMainScreen ?: return
        screen = stored
        storedMainScreen = null
        storedMainCursor?.let { cursor ->
            row = cursor.row.coerceIn(0, rows - 1)
            column = cursor.column.coerceIn(0, cols - 1)
            style = cursor.style
        }
        storedMainCursor = null
        pendingWrap = false
    }

    private fun pushToScrollback(line: Row) {
        scrollback.addLast(line.toLine())
        while (scrollback.size > scrollbackLimit) scrollback.removeFirst()
    }

    private fun eraseStyle(): CellStyle = CellStyle(background = style.background)

    private fun blankRows(cols: Int, rows: Int): MutableList<Row> =
        MutableList(rows) { Row(cols) }

    private data class Cursor(val row: Int, val column: Int, val style: CellStyle)

    private data class ExtendedColor(val color: TerminalColor?, val consumed: Int)

    private enum class State { GROUND, ESCAPE, CSI, OSC, OSC_ESCAPE, STRING, STRING_ESCAPE, CONSUME_ONE }

    private class Row(cols: Int) {

        var characters: CharArray = CharArray(cols) { ' ' }
            private set
        var styles: Array<CellStyle> = Array(cols) { CellStyle.DEFAULT }
            private set

        val size: Int get() = characters.size

        fun set(index: Int, char: Char, style: CellStyle) {
            if (index !in characters.indices) return
            characters[index] = char
            styles[index] = style
        }

        fun clear(from: Int, to: Int, style: CellStyle) {
            for (index in maxOf(from, 0)..minOf(to, size - 1)) {
                characters[index] = ' '
                styles[index] = style
            }
        }

        fun insert(at: Int, count: Int, style: CellStyle) {
            if (at !in characters.indices || count <= 0) return
            val shift = minOf(count, size - at)
            for (index in size - 1 downTo at + shift) {
                characters[index] = characters[index - shift]
                styles[index] = styles[index - shift]
            }
            clear(at, at + shift - 1, style)
        }

        fun delete(at: Int, count: Int, style: CellStyle) {
            if (at !in characters.indices || count <= 0) return
            val shift = minOf(count, size - at)
            for (index in at until size - shift) {
                characters[index] = characters[index + shift]
                styles[index] = styles[index + shift]
            }
            clear(size - shift, size - 1, style)
        }

        fun resized(newCols: Int): Row {
            if (newCols == size) return this
            val copy = Row(newCols)
            for (index in 0 until minOf(size, newCols)) {
                copy.characters[index] = characters[index]
                copy.styles[index] = styles[index]
            }
            return copy
        }

        fun toLine(): TerminalLine {
            var last = -1
            for (index in characters.indices) {
                if (characters[index] != ' ' || styles[index] != CellStyle.DEFAULT) last = index
            }
            if (last < 0) return TerminalLine.EMPTY
            val runs = ArrayList<StyledRun>()
            var start = 0
            while (start <= last) {
                var end = start
                while (end + 1 <= last && styles[end + 1] == styles[start]) end++
                runs.add(StyledRun(String(characters, start, end - start + 1), styles[start]))
                start = end + 1
            }
            return TerminalLine(runs)
        }
    }

    private companion object {
        const val TAB_WIDTH = 8
        const val BELL = 0x07
        const val BACKSPACE = 0x08
        const val TAB = 0x09
        const val LINE_FEED = 0x0A
        const val VERTICAL_TAB = 0x0B
        const val FORM_FEED = 0x0C
        const val CARRIAGE_RETURN = 0x0D
        const val ESCAPE_CODE = 0x1B
        const val DELETE = 0x7F
    }
}
