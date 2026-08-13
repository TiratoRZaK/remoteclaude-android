package dev.rclaude.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionLinkTest {

    @Test
    fun `разбирает ссылку из QR с токеном во фрагменте`() {
        val link = ConnectionLink.parse("http://192.168.1.40:7777/#t=8f3ac1").getOrThrow()

        assertEquals("http", link.address.scheme)
        assertEquals("192.168.1.40", link.address.host)
        assertEquals(7777, link.address.port)
        assertEquals("8f3ac1", link.token)
        assertEquals("http://192.168.1.40:7777", link.address.httpBase)
        assertEquals("ws://192.168.1.40:7777", link.address.wsBase)
    }

    @Test
    fun `схему можно опустить`() {
        val link = ConnectionLink.parse("192.168.1.40:7777/#t=abc").getOrThrow()

        assertEquals("http", link.address.scheme)
        assertEquals("192.168.1.40", link.address.host)
        assertEquals(7777, link.address.port)
        assertEquals("abc", link.token)
    }

    @Test
    fun `без порта берётся порт сервера по умолчанию`() {
        val link = ConnectionLink.parse("http://home-pc/#t=abc").getOrThrow()

        assertEquals(ConnectionLink.DEFAULT_PORT, link.address.port)
    }

    @Test
    fun `https даёт защищённый websocket`() {
        val link = ConnectionLink.parse("https://pc.local:8443/#t=abc").getOrThrow()

        assertEquals("wss://pc.local:8443", link.address.wsBase)
    }

    @Test
    fun `токен читается и из query`() {
        val link = ConnectionLink.parse("http://10.0.0.2:7777/?token=zzz").getOrThrow()

        assertEquals("zzz", link.token)
    }

    @Test
    fun `фрагмент важнее query`() {
        val link = ConnectionLink.parse("http://10.0.0.2:7777/?token=from-query#t=from-hash").getOrThrow()

        assertEquals("from-hash", link.token)
    }

    @Test
    fun `токен декодируется из процентной записи`() {
        val link = ConnectionLink.parse("http://10.0.0.2:7777/#t=a%2Fb%3Dc").getOrThrow()

        assertEquals("a/b=c", link.token)
    }

    @Test
    fun `ссылка без токена разбирается, токен пустой`() {
        val link = ConnectionLink.parse("  http://10.0.0.2:7777/  ").getOrThrow()

        assertEquals("10.0.0.2", link.address.host)
        assertNull(link.token)
    }

    @Test
    fun `адрес IPv6 сохраняет скобки`() {
        val link = ConnectionLink.parse("http://[fe80::1]:7777/#t=abc").getOrThrow()

        assertEquals("http://[fe80::1]:7777", link.address.httpBase)
        assertEquals("abc", link.token)
    }

    @Test
    fun `пустая строка — ошибка`() {
        val error = ConnectionLink.parse("   ").exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("пустая ссылка", error.message)
    }

    @Test
    fun `чужая схема — ошибка`() {
        val error = ConnectionLink.parse("ftp://10.0.0.2:7777/").exceptionOrNull()

        assertEquals("нужна ссылка http:// или https://", error?.message)
    }

    @Test
    fun `ссылка без хоста — ошибка`() {
        val error = ConnectionLink.parse("http://:7777/#t=abc").exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
