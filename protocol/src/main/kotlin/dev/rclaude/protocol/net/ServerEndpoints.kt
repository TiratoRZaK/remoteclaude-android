package dev.rclaude.protocol.net

import dev.rclaude.protocol.ServerAddress
import dev.rclaude.protocol.UrlCoding

/** Адреса точек входа сервера remoteclaude. */
object ServerEndpoints {

    /** Проверка живости сервера — единственный запрос без токена. */
    fun health(address: ServerAddress): String = "${address.httpBase}/api/health"

    /** Список живых сессий. */
    fun sessions(address: ServerAddress): String = "${address.httpBase}/api/sessions"

    /** WebSocket сессии: поток вывода PTY, чат-события и ввод. */
    fun sessionSocket(address: ServerAddress, sessionId: String, token: String): String =
        "${address.wsBase}/ws?session=${encode(sessionId)}&token=${encode(token)}"

    private fun encode(value: String): String = UrlCoding.encode(value)
}
