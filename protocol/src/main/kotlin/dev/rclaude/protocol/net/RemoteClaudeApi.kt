package dev.rclaude.protocol.net

import dev.rclaude.protocol.ProtocolJson
import dev.rclaude.protocol.ServerAddress
import dev.rclaude.protocol.ServerHealth
import dev.rclaude.protocol.SessionInfo
import dev.rclaude.protocol.UrlCoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Ошибка обращения к серверу remoteclaude с текстом для показа пользователю. */
class ApiException(message: String, val statusCode: Int? = null) : IOException(message)

/** HTTP-часть протокола: живость сервера и список сессий. */
class RemoteClaudeApi(private val client: OkHttpClient = defaultClient()) {

    /** `GET /api/health` — токен не требуется. */
    suspend fun health(address: ServerAddress): ServerHealth = withContext(Dispatchers.IO) {
        val body = get(ServerEndpoints.health(address), token = null)
        try {
            ProtocolJson.parseHealth(body)
        } catch (e: Exception) {
            throw ApiException("по этому адресу отвечает не remoteclaude")
        }
    }

    /** `GET /api/sessions` — список живых сессий. */
    suspend fun sessions(address: ServerAddress, token: String): List<SessionInfo> = withContext(Dispatchers.IO) {
        val body = get(ServerEndpoints.sessions(address), token)
        try {
            ProtocolJson.parseSessions(body)
        } catch (e: Exception) {
            throw ApiException("сервер вернул неожиданный ответ на список сессий")
        }
    }

    private fun get(url: String, token: String?): String {
        // Заголовок принимает только печатные ASCII-символы; токен с чем-то иным
        // сервер так же примет параметром token (см. isAuthorized на сервере).
        val headerToken = token?.takeIf { value -> value.all { it.code in 0x21..0x7E } }
        val builder = Request.Builder()
            .url(if (token != null && headerToken == null) withTokenParameter(url, token) else url)
            .get()
        if (headerToken != null) builder.header("Authorization", "Bearer $headerToken")
        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw ApiException("сервер недоступен: ${e.message ?: "нет соединения"}")
        }
        response.use {
            val body = it.body.string()
            when {
                it.code == 401 -> throw ApiException("нет доступа: сервер не принял токен", 401)
                !it.isSuccessful -> throw ApiException("сервер ответил ${it.code}", it.code)
                else -> return body
            }
        }
    }

    private fun withTokenParameter(url: String, token: String): String {
        val separator = if (url.contains('?')) '&' else '?'
        return url + separator + "token=" + UrlCoding.encode(token)
    }

    companion object {

        /** Клиент для коротких HTTP-запросов в локальной сети. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        /** Клиент для WebSocket: без таймаута чтения, с пингом для живучести на Wi-Fi. */
        fun socketClient(base: OkHttpClient = defaultClient()): OkHttpClient = base.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
