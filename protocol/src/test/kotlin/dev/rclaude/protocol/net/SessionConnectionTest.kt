package dev.rclaude.protocol.net

import dev.rclaude.protocol.ClientMessage
import dev.rclaude.protocol.ServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionConnectionTest {

    private class FakeSocket : SessionSocket {
        val sent = mutableListOf<ClientMessage>()
        var closed = false
        var accepting = true

        override fun send(message: ClientMessage): Boolean {
            if (!accepting) return false
            sent += message
            return true
        }

        override fun close() {
            closed = true
        }
    }

    private class FakeFactory : SessionSocketFactory {
        val urls = mutableListOf<String>()
        val sockets = mutableListOf<FakeSocket>()
        val listeners = mutableListOf<SessionSocketListener>()

        override fun open(url: String, listener: SessionSocketListener): SessionSocket {
            urls += url
            listeners += listener
            return FakeSocket().also { sockets += it }
        }
    }

    @Test
    fun `отдаёт вывод и разобранные кадры`() = runTest {
        val factory = FakeFactory()
        val connection = SessionConnection(factory)
        val events = mutableListOf<ConnectionEvent>()
        backgroundScope.launch { connection.events(URL).collect { events += it } }
        runCurrent()

        factory.listeners[0].onOpen()
        factory.listeners[0].onBinary("вывод".toByteArray())
        factory.listeners[0].onText("""{"type":"status","menuWaiting":true}""")
        factory.listeners[0].onText("{битый json")
        runCurrent()

        assertEquals(URL, factory.urls.single())
        assertEquals(3, events.size)
        assertIs<ConnectionEvent.Connected>(events[0])
        assertEquals("вывод", String(assertIs<ConnectionEvent.Output>(events[1]).bytes))
        val status = assertIs<ServerMessage.Status>(assertIs<ConnectionEvent.Incoming>(events[2]).message)
        assertTrue(status.menuWaiting)
    }

    @Test
    fun `после обрыва переподключается с нарастающей паузой`() = runTest {
        val factory = FakeFactory()
        val connection = SessionConnection(factory)
        val events = mutableListOf<ConnectionEvent>()
        backgroundScope.launch { connection.events(URL).collect { events += it } }
        runCurrent()

        factory.listeners[0].onFailure(IllegalStateException("сеть пропала"))
        runCurrent()
        assertEquals(1_000L, assertIs<ConnectionEvent.Reconnecting>(events.last()).delayMs)
        assertEquals("сеть пропала", assertIs<ConnectionEvent.Reconnecting>(events.last()).reason)
        assertTrue(factory.sockets[0].closed)

        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(2, factory.listeners.size)

        factory.listeners[1].onClosed(null)
        runCurrent()
        assertEquals(2_000L, assertIs<ConnectionEvent.Reconnecting>(events.last()).delayMs)
    }

    @Test
    fun `удачное подключение возвращает паузу к началу`() = runTest {
        val factory = FakeFactory()
        val connection = SessionConnection(factory)
        val events = mutableListOf<ConnectionEvent>()
        backgroundScope.launch { connection.events(URL).collect { events += it } }
        runCurrent()

        factory.listeners[0].onClosed("раз")
        runCurrent()
        advanceTimeBy(1_001)
        runCurrent()
        factory.listeners[1].onOpen()
        factory.listeners[1].onClosed("два")
        runCurrent()

        val delays = events.filterIsInstance<ConnectionEvent.Reconnecting>().map { it.delayMs }
        assertEquals(listOf(1_000L, 1_000L), delays)
    }

    @Test
    fun `кадр exit завершает поток без переподключения`() = runTest {
        val factory = FakeFactory()
        val connection = SessionConnection(factory)
        val events = mutableListOf<ConnectionEvent>()
        val job = backgroundScope.launch { connection.events(URL).collect { events += it } }
        runCurrent()

        factory.listeners[0].onOpen()
        factory.listeners[0].onText("""{"type":"exit","code":0}""")
        factory.listeners[0].onClosed(null)
        advanceTimeBy(30_000)
        runCurrent()

        assertTrue(job.isCompleted)
        assertEquals(1, factory.listeners.size)
        assertTrue(factory.sockets[0].closed)
        assertIs<ServerMessage.Exit>(assertIs<ConnectionEvent.Incoming>(events.last()).message)
    }

    @Test
    fun `ввод уходит в открытый сокет`() = runTest {
        val factory = FakeFactory()
        val connection = SessionConnection(factory)
        assertFalse(connection.send(ClientMessage.Input("нет сокета")))

        backgroundScope.launch { connection.events(URL).collect { } }
        runCurrent()

        assertTrue(connection.send(ClientMessage.Input("привет")))
        assertTrue(connection.send(ClientMessage.Resize(100, 40)))
        assertEquals(
            listOf(ClientMessage.Input("привет"), ClientMessage.Resize(100, 40)),
            factory.sockets[0].sent,
        )
    }

    @Test
    fun `отмена сбора закрывает сокет`() = runTest {
        val factory = FakeFactory()
        val connection = SessionConnection(factory)
        val job = backgroundScope.launch { connection.events(URL).collect { } }
        runCurrent()

        job.cancel()
        runCurrent()

        assertTrue(factory.sockets[0].closed)
        assertFalse(connection.connected)
    }

    private companion object {
        const val URL = "ws://127.0.0.1:7777/ws?session=a1&token=t"
    }
}
