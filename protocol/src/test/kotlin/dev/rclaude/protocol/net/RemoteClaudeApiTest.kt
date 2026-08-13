package dev.rclaude.protocol.net

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.rclaude.protocol.ServerAddress
import kotlinx.coroutines.test.runTest
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RemoteClaudeApiTest {

    private class TestServer(handler: (HttpExchange) -> Unit) : AutoCloseable {
        private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

        val address: ServerAddress

        init {
            server.createContext("/", handler)
            server.start()
            address = ServerAddress("http", "127.0.0.1", server.address.port)
        }

        override fun close() = server.stop(0)
    }

    private fun HttpExchange.reply(code: Int, body: String) {
        val bytes = body.toByteArray()
        responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        sendResponseHeaders(code, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    @Test
    fun `health отдаёт версию сервера`() = runTest {
        TestServer { exchange ->
            assertEquals("/api/health", exchange.requestURI.path)
            exchange.reply(200, """{"ok":true,"version":"0.1.0"}""")
        }.use { server ->
            val health = RemoteClaudeApi().health(server.address)

            assertTrue(health.ok)
            assertEquals("0.1.0", health.version)
        }
    }

    @Test
    fun `список сессий запрашивается с токеном в заголовке`() = runTest {
        var authorization: String? = null
        TestServer { exchange ->
            authorization = exchange.requestHeaders.getFirst("Authorization")
            exchange.reply(
                200,
                """[{"id":"a1","name":"sphere-1","cwd":"W:\\Work","cols":120,"rows":30,"viewers":1,"chat":true}]""",
            )
        }.use { server ->
            val sessions = RemoteClaudeApi().sessions(server.address, "8f3ac1")

            assertEquals("Bearer 8f3ac1", authorization)
            assertEquals(1, sessions.size)
            assertEquals("sphere-1", sessions[0].name)
            assertEquals(120, sessions[0].cols)
            assertTrue(sessions[0].chat)
        }
    }

    @Test
    fun `токен вне ASCII уходит параметром запроса`() = runTest {
        var authorization: String? = null
        var query: String? = null
        TestServer { exchange ->
            authorization = exchange.requestHeaders.getFirst("Authorization")
            query = exchange.requestURI.rawQuery
            exchange.reply(200, "[]")
        }.use { server ->
            RemoteClaudeApi().sessions(server.address, "секрет")

            assertEquals(null, authorization)
            assertEquals("token=%D1%81%D0%B5%D0%BA%D1%80%D0%B5%D1%82", query)
        }
    }

    @Test
    fun `неверный токен даёт понятную ошибку`() = runTest {
        TestServer { exchange -> exchange.reply(401, """{"error":"нет доступа"}""") }.use { server ->
            val error = assertFailsWith<ApiException> { RemoteClaudeApi().sessions(server.address, "bad") }

            assertEquals(401, error.statusCode)
            assertEquals("нет доступа: сервер не принял токен", error.message)
        }
    }

    @Test
    fun `чужой сервер по адресу распознаётся`() = runTest {
        TestServer { exchange -> exchange.reply(200, "<html>привет</html>") }.use { server ->
            val error = assertFailsWith<ApiException> { RemoteClaudeApi().health(server.address) }

            assertEquals("по этому адресу отвечает не remoteclaude", error.message)
        }
    }

    @Test
    fun `недоступный сервер даёт ошибку соединения`() = runTest {
        val closedPort = TestServer { }.use { it.address }

        val error = assertFailsWith<ApiException> { RemoteClaudeApi().health(closedPort) }

        assertTrue(error.message!!.startsWith("сервер недоступен"))
    }

    @Test
    fun `адреса точек входа собираются по протоколу`() {
        val address = ServerAddress("http", "192.168.1.40", 7777)

        assertEquals("http://192.168.1.40:7777/api/health", ServerEndpoints.health(address))
        assertEquals("http://192.168.1.40:7777/api/sessions", ServerEndpoints.sessions(address))
        assertEquals(
            "ws://192.168.1.40:7777/ws?session=a1&token=a%2Fb",
            ServerEndpoints.sessionSocket(address, "a1", "a/b"),
        )
    }
}
