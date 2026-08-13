package dev.rclaude.protocol.term

/**
 * Потоковый декодер UTF-8: многобайтовый символ, разрезанный границей кадра
 * WebSocket, дочитывается следующей порцией. Битые байты дают U+FFFD.
 */
class Utf8StreamDecoder {

    private var pending: ByteArray = EMPTY

    /** Декодирует очередную порцию байтов, удерживая незавершённый хвост. */
    fun decode(bytes: ByteArray): String {
        val input = if (pending.isEmpty()) bytes else pending + bytes
        pending = EMPTY
        val out = StringBuilder(input.size)
        var i = 0
        while (i < input.size) {
            val lead = input[i].toInt() and 0xFF
            val length = when {
                lead < 0x80 -> 1
                lead in 0xC2..0xDF -> 2
                lead in 0xE0..0xEF -> 3
                lead in 0xF0..0xF4 -> 4
                else -> 0
            }
            if (length == 0) {
                out.append(REPLACEMENT)
                i++
                continue
            }
            if (length == 1) {
                out.append(lead.toChar())
                i++
                continue
            }
            if (i + length > input.size) {
                pending = input.copyOfRange(i, input.size)
                break
            }
            var codePoint = when (length) {
                2 -> lead and 0x1F
                3 -> lead and 0x0F
                else -> lead and 0x07
            }
            var valid = true
            for (k in 1 until length) {
                val next = input[i + k].toInt() and 0xFF
                if (next and 0xC0 != 0x80) {
                    valid = false
                    break
                }
                codePoint = (codePoint shl 6) or (next and 0x3F)
            }
            if (!valid) {
                out.append(REPLACEMENT)
                i++
                continue
            }
            val overlong = (length == 3 && codePoint < 0x800) || (length == 4 && codePoint < 0x10000)
            val illegal = codePoint in 0xD800..0xDFFF || codePoint > 0x10FFFF
            if (overlong || illegal) {
                out.append(REPLACEMENT)
            } else if (codePoint <= 0xFFFF) {
                out.append(codePoint.toChar())
            } else {
                val shifted = codePoint - 0x10000
                out.append((0xD800 + (shifted shr 10)).toChar())
                out.append((0xDC00 + (shifted and 0x3FF)).toChar())
            }
            i += length
        }
        return out.toString()
    }

    /** Забывает незавершённый хвост — при переподключении поток начинается заново. */
    fun reset() {
        pending = EMPTY
    }

    private companion object {
        const val REPLACEMENT = '�'
        val EMPTY = ByteArray(0)
    }
}
