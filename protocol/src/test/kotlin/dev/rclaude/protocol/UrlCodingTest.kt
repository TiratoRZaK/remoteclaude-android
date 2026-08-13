package dev.rclaude.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlCodingTest {

    @Test
    fun `безопасные символы остаются как есть`() {
        assertEquals("8f3ac1-Az_0.~", UrlCoding.encode("8f3ac1-Az_0.~"))
    }

    @Test
    fun `служебные символы кодируются процентами в верхнем регистре`() {
        assertEquals("a%2Fb%3Dc", UrlCoding.encode("a/b=c"))
        assertEquals("%20", UrlCoding.encode(" "))
    }

    @Test
    fun `кириллица кодируется в UTF-8`() {
        assertEquals("%D1%81%D0%B5%D0%BA%D1%80%D0%B5%D1%82", UrlCoding.encode("секрет"))
    }

    @Test
    fun `декодирование возвращает исходную строку`() {
        for (value in listOf("секрет", "a/b=c", "8f3ac1", "🚀 ключ", "")) {
            assertEquals(value, UrlCoding.decode(UrlCoding.encode(value)))
        }
    }

    @Test
    fun `плюс читается как пробел`() {
        assertEquals("два слова", UrlCoding.decode("два+слова"))
    }

    @Test
    fun `многобайтовая последовательность собирается из нескольких процентов`() {
        assertEquals("привет", UrlCoding.decode("%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82"))
    }

    @Test
    fun `битая процентная запись остаётся текстом`() {
        assertEquals("100% готово", UrlCoding.decode("100% готово"))
        assertEquals("хвост%", UrlCoding.decode("хвост%"))
        assertEquals("%zz", UrlCoding.decode("%zz"))
    }
}
