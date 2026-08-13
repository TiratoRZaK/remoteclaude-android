package dev.rclaude.protocol.net

import dev.rclaude.protocol.ClientMessage
import dev.rclaude.protocol.ProtocolJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/** Открытый WebSocket сессии. */
interface SessionSocket {

    /** Отправляет кадр клиента; `false` — сокет уже не принимает данные. */
    fun send(message: ClientMessage): Boolean

    /** Закрывает соединение. */
    fun close()
}

/** События сокета от сервера. */
interface SessionSocketListener {
    fun onOpen()

    /** Сырые байты вывода PTY. */
    fun onBinary(bytes: ByteArray)

    /** Текстовый кадр протокола. */
    fun onText(text: String)

    fun onClosed(reason: String?)

    fun onFailure(error: Throwable)
}

/** Фабрика сокетов — точка подмены в тестах. */
fun interface SessionSocketFactory {
    fun open(url: String, listener: SessionSocketListener): SessionSocket
}

/** Фабрика поверх OkHttp: `ws://`-адрес принимается как есть. */
class OkHttpSessionSocketFactory(
    private val client: OkHttpClient = RemoteClaudeApi.socketClient(),
) : SessionSocketFactory {

    override fun open(url: String, listener: SessionSocketListener): SessionSocket {
        val request = Request.Builder().url(url).build()
        val socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = listener.onOpen()

                override fun onMessage(webSocket: WebSocket, text: String) = listener.onText(text)

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) =
                    listener.onBinary(bytes.toByteArray())

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(NORMAL_CLOSURE, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) =
                    listener.onClosed(reason.ifEmpty { null })

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                    listener.onFailure(t)
            },
        )
        return OkHttpSession(socket)
    }

    private class OkHttpSession(private val socket: WebSocket) : SessionSocket {

        override fun send(message: ClientMessage): Boolean = socket.send(ProtocolJson.encode(message))

        override fun close() {
            socket.close(NORMAL_CLOSURE, null)
        }
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
