package dev.rclaude.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolJsonTest {

    @Test
    fun `чат-кадр со сбросом ленты`() {
        val text = """
            {"type":"chat","reset":true,"events":[
              {"kind":"user","text":"привет","ts":"2026-08-13T10:00:00.000Z"},
              {"kind":"assistant","text":"здравствуйте","ts":"2026-08-13T10:00:01.000Z"},
              {"kind":"tool","name":"Bash","detail":"npm test","ts":"2026-08-13T10:00:02.000Z"}
            ]}
        """.trimIndent()

        val message = assertIs<ServerMessage.Chat>(ProtocolJson.parseServerMessage(text))

        assertTrue(message.reset)
        assertEquals(3, message.events.size)
        assertEquals(ChatKind.USER, message.events[0].kind)
        assertEquals("привет", message.events[0].text)
        assertEquals("2026-08-13T10:00:00.000Z", message.events[0].ts)
        assertEquals(ChatKind.TOOL, message.events[2].kind)
        assertEquals("Bash · npm test", message.events[2].display)
    }

    @Test
    fun `чат-кадр о недоступном транскрипте`() {
        val message = assertIs<ServerMessage.Chat>(ProtocolJson.parseServerMessage("""{"type":"chat","unavailable":true}"""))

        assertTrue(message.unavailable)
        assertTrue(message.events.isEmpty())
    }

    @Test
    fun `испорченное событие пропускается, соседние остаются`() {
        val text = """
            {"type":"chat","events":[
              {"kind":"thinking","text":"размышление"},
              {"kind":"user"},
              {"kind":"assistant","text":"ответ"}
            ]}
        """.trimIndent()

        val message = assertIs<ServerMessage.Chat>(ProtocolJson.parseServerMessage(text))

        assertEquals(1, message.events.size)
        assertEquals("ответ", message.events[0].text)
    }

    @Test
    fun `кадр состояния меню`() {
        val message = assertIs<ServerMessage.Status>(ProtocolJson.parseServerMessage("""{"type":"status","menuWaiting":true}"""))

        assertTrue(message.menuWaiting)
    }

    @Test
    fun `кадр завершения сессии`() {
        val message = assertIs<ServerMessage.Exit>(ProtocolJson.parseServerMessage("""{"type":"exit","code":143}"""))

        assertEquals(143, message.code)
    }

    @Test
    fun `кадр ошибки сессии`() {
        val message = assertIs<ServerMessage.Failure>(ProtocolJson.parseServerMessage("""{"type":"error","message":"сессия не найдена"}"""))

        assertEquals("сессия не найдена", message.message)
    }

    @Test
    fun `незнакомый тип кадра не роняет разбор`() {
        val message = assertIs<ServerMessage.Unknown>(ProtocolJson.parseServerMessage("""{"type":"future","payload":42}"""))

        assertEquals("future", message.type)
    }

    @Test
    fun `неизвестные поля игнорируются`() {
        val message = assertIs<ServerMessage.Status>(
            ProtocolJson.parseServerMessage("""{"type":"status","menuWaiting":false,"extra":{"a":1}}"""),
        )

        assertEquals(false, message.menuWaiting)
    }

    @Test
    fun `битый JSON даёт null`() {
        assertNull(ProtocolJson.parseServerMessage("{не json"))
        assertNull(ProtocolJson.parseServerMessage("[]"))
        assertNull(ProtocolJson.parseServerMessage("""{"code":1}"""))
    }

    @Test
    fun `ввод сериализуется в кадр input`() {
        val encoded = ProtocolJson.encode(ClientMessage.Input(TerminalKeys.UP))

        assertEquals("""{"type":"input","data":"\u001b[A"}""", encoded)
    }

    @Test
    fun `изменение размера сериализуется в кадр resize`() {
        assertEquals(
            """{"type":"resize","cols":100,"rows":40}""",
            ProtocolJson.encode(ClientMessage.Resize(100, 40)),
        )
    }

    @Test
    fun `список сессий разбирается, запись без id отбрасывается`() {
        val body = """
            [
              {"id":"a1","name":"sphere-1","cwd":"W:\\Work","cols":120,"rows":30,
               "startedAt":"2026-08-13T09:00:00.000Z","viewers":2,"chat":true},
              {"name":"без id"},
              {"id":"b2","name":"vzp-1","cwd":"W:\\Other","cols":80,"rows":24,
               "startedAt":"2026-08-13T09:10:00.000Z","viewers":0,"chat":false,"unknown":1}
            ]
        """.trimIndent()

        val sessions = ProtocolJson.parseSessions(body)

        assertEquals(2, sessions.size)
        assertEquals("a1", sessions[0].id)
        assertEquals(120, sessions[0].cols)
        assertEquals(2, sessions[0].viewers)
        assertTrue(sessions[0].chat)
        assertEquals("b2", sessions[1].id)
        assertEquals(false, sessions[1].chat)
    }

    @Test
    fun `health разбирается`() {
        val health = ProtocolJson.parseHealth("""{"ok":true,"version":"0.1.0"}""")

        assertTrue(health.ok)
        assertEquals("0.1.0", health.version)
    }
}
