package dev.rclaude.protocol.term

import kotlin.test.Test
import kotlin.test.assertEquals

class Utf8StreamDecoderTest {

    @Test
    fun `однобайтовый текст`() {
        assertEquals("hello", Utf8StreamDecoder().decode("hello".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `двухбайтовый символ на границе кадров`() {
        val decoder = Utf8StreamDecoder()
        val bytes = "привет".toByteArray(Charsets.UTF_8)

        val first = decoder.decode(bytes.copyOfRange(0, 3))
        val second = decoder.decode(bytes.copyOfRange(3, bytes.size))

        assertEquals("п", first)
        assertEquals("ривет", second)
    }

    @Test
    fun `четырёхбайтовый символ собирается по одному байту`() {
        val decoder = Utf8StreamDecoder()
        val bytes = "🚀".toByteArray(Charsets.UTF_8)
        val out = StringBuilder()

        for (byte in bytes) out.append(decoder.decode(byteArrayOf(byte)))

        assertEquals("🚀", out.toString())
    }

    @Test
    fun `битый байт заменяется на символ замены`() {
        val decoded = Utf8StreamDecoder().decode(byteArrayOf(0x61, 0xFF.toByte(), 0x62))

        assertEquals("a�b", decoded)
    }

    @Test
    fun `оборванная последовательность не теряет следующий текст`() {
        val decoder = Utf8StreamDecoder()

        val first = decoder.decode(byteArrayOf(0xD0.toByte()))
        val second = decoder.decode(byteArrayOf(0x61))

        assertEquals("", first)
        assertEquals("�a", second)
    }

    @Test
    fun `сброс забывает незавершённый хвост`() {
        val decoder = Utf8StreamDecoder()
        decoder.decode("п".toByteArray(Charsets.UTF_8).copyOfRange(0, 1))

        decoder.reset()

        assertEquals("a", decoder.decode(byteArrayOf(0x61)))
    }
}
