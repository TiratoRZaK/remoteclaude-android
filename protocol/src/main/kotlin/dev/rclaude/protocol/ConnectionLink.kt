package dev.rclaude.protocol

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Адрес сервера remoteclaude: схема, хост и порт. */
data class ServerAddress(val scheme: String, val host: String, val port: Int) {

    /** База для HTTP-запросов, например `http://192.168.1.40:7777`. */
    val httpBase: String get() = "$scheme://$host:$port"

    /** База для WebSocket-подключений, например `ws://192.168.1.40:7777`. */
    val wsBase: String get() = "${if (scheme == "https") "wss" else "ws"}://$host:$port"

    override fun toString(): String = httpBase
}

/**
 * Разобранная ссылка подключения вида `http://192.168.1.40:7777/#t=<токен>`:
 * адрес сервера и токен доступа (во фрагменте `#t=`/`#token=` либо в query `?token=`).
 */
data class ConnectionLink(val address: ServerAddress, val token: String?) {

    companion object {
        /** Порт сервера remoteclaude по умолчанию. */
        const val DEFAULT_PORT: Int = 7777

        private val SCHEMES = setOf("http", "https")

        /**
         * Разбирает ссылку подключения. Схему можно опустить — подставляется `http`,
         * порт по умолчанию — [DEFAULT_PORT].
         */
        fun parse(raw: String): Result<ConnectionLink> {
            val text = raw.trim()
            if (text.isEmpty()) return failure("пустая ссылка")
            val normalized = if (text.contains("://")) text else "http://$text"
            val uri = try {
                URI(normalized)
            } catch (e: Exception) {
                return failure("ссылку не разобрать: ${e.message}")
            }
            val scheme = uri.scheme?.lowercase()
            if (scheme == null || scheme !in SCHEMES) return failure("нужна ссылка http:// или https://")
            val host = uri.host
            if (host.isNullOrBlank()) return failure("в ссылке нет адреса сервера")
            val port = if (uri.port > 0) uri.port else DEFAULT_PORT
            val token = tokenFrom(uri.rawFragment) ?: tokenFrom(uri.rawQuery)
            return Result.success(ConnectionLink(ServerAddress(scheme, host, port), token))
        }

        private fun failure(message: String): Result<ConnectionLink> =
            Result.failure(IllegalArgumentException(message))

        private fun tokenFrom(rawParams: String?): String? {
            if (rawParams.isNullOrEmpty()) return null
            for (pair in rawParams.split('&')) {
                val separator = pair.indexOf('=')
                if (separator <= 0) continue
                val name = pair.substring(0, separator)
                if (name != "t" && name != "token") continue
                val value = decode(pair.substring(separator + 1))
                if (value.isNotBlank()) return value
            }
            return null
        }

        private fun decode(value: String): String = try {
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            value
        }
    }
}
