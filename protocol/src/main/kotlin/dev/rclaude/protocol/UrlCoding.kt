package dev.rclaude.protocol

import java.io.ByteArrayOutputStream

/**
 * Процентное кодирование параметров ссылки своими руками.
 *
 * `URLEncoder`/`URLDecoder` с параметром `Charset` появились только в API 33, а на
 * старых Android их вызов падает с `NoSuchMethodError`. Здесь тот же результат без
 * оглядки на версию платформы: кодировка всегда UTF-8, `+` при разборе читается как
 * пробел — так же ведёт себя `URLSearchParams` в браузере и сервер remoteclaude.
 */
object UrlCoding {

    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
    private const val HEX = "0123456789ABCDEF"

    /** Кодирует значение параметра запроса. */
    fun encode(value: String): String = buildString(value.length) {
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val code = byte.toInt() and 0xFF
            val char = code.toChar()
            if (char in UNRESERVED) {
                append(char)
            } else {
                append('%').append(HEX[code shr 4]).append(HEX[code and 0x0F])
            }
        }
    }

    /** Декодирует значение параметра запроса или фрагмента. */
    fun decode(value: String): String {
        if ('%' !in value && '+' !in value) return value
        val text = StringBuilder(value.length)
        val bytes = ByteArrayOutputStream()

        fun flushBytes() {
            if (bytes.size() > 0) {
                text.append(String(bytes.toByteArray(), Charsets.UTF_8))
                bytes.reset()
            }
        }

        var index = 0
        while (index < value.length) {
            val char = value[index]
            val high = if (char == '%' && index + 2 < value.length) hex(value[index + 1]) else -1
            val low = if (high >= 0) hex(value[index + 2]) else -1
            when {
                low >= 0 -> {
                    bytes.write(high * 16 + low)
                    index += 3
                }

                char == '+' -> {
                    flushBytes()
                    text.append(' ')
                    index++
                }

                else -> {
                    flushBytes()
                    text.append(char)
                    index++
                }
            }
        }
        flushBytes()
        return text.toString()
    }

    private fun hex(char: Char): Int = when (char) {
        in '0'..'9' -> char - '0'
        in 'a'..'f' -> char - 'a' + 10
        in 'A'..'F' -> char - 'A' + 10
        else -> -1
    }
}
