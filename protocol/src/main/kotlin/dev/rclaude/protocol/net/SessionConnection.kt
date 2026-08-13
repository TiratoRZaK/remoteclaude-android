package dev.rclaude.protocol.net

import dev.rclaude.protocol.ClientMessage
import dev.rclaude.protocol.ProtocolJson
import dev.rclaude.protocol.ReconnectPolicy
import dev.rclaude.protocol.ServerMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/** Событие живого подключения к сессии. */
sealed interface ConnectionEvent {

    /** Сокет открыт: сервер сейчас проиграет буфер вывода и накопленную ленту. */
    data object Connected : ConnectionEvent

    /** Порция байтов вывода PTY. */
    class Output(val bytes: ByteArray) : ConnectionEvent

    /** Разобранный текстовый кадр сервера. */
    data class Incoming(val message: ServerMessage) : ConnectionEvent

    /** Связь потеряна, следующая попытка через [delayMs]. */
    data class Reconnecting(val delayMs: Long, val reason: String? = null) : ConnectionEvent
}

/**
 * Подключение к сессии с автопереподключением: цикл «открыть сокет — отдавать события —
 * подождать нарастающую паузу» живёт, пока подписчик собирает поток. Кадр `exit`
 * завершает поток без новых попыток.
 */
class SessionConnection(
    private val factory: SessionSocketFactory,
    private val policy: ReconnectPolicy = ReconnectPolicy(),
) {

    @Volatile
    private var socket: SessionSocket? = null

    /** Подключён ли сокет прямо сейчас. */
    val connected: Boolean get() = socket != null

    /** Поток событий сессии; отмена сбора закрывает сокет. */
    fun events(url: String): Flow<ConnectionEvent> = channelFlow {
        var finished = false
        try {
            while (!finished) {
                val signals = Channel<Signal>(Channel.UNLIMITED)
                val current = factory.open(url, ChannelListener(signals))
                socket = current
                var reason: String? = null
                try {
                    for (signal in signals) {
                        when (signal) {
                            Signal.Open -> {
                                policy.reset()
                                send(ConnectionEvent.Connected)
                            }

                            is Signal.Binary -> send(ConnectionEvent.Output(signal.bytes))
                            is Signal.Text -> {
                                val message = ProtocolJson.parseServerMessage(signal.text)
                                if (message != null) {
                                    send(ConnectionEvent.Incoming(message))
                                    if (message is ServerMessage.Exit) finished = true
                                }
                            }

                            is Signal.Ended -> {
                                reason = signal.reason
                                break
                            }
                        }
                    }
                } finally {
                    socket = null
                    current.close()
                }
                if (finished) break
                val delayMs = policy.nextDelayMs()
                send(ConnectionEvent.Reconnecting(delayMs, reason))
                delay(delayMs)
            }
        } finally {
            socket = null
        }
    }

    /** Отправляет кадр в сессию; `false` — сокет закрыт, кадр не ушёл. */
    fun send(message: ClientMessage): Boolean = socket?.send(message) ?: false

    private class ChannelListener(private val signals: Channel<Signal>) : SessionSocketListener {

        override fun onOpen() {
            signals.trySend(Signal.Open)
        }

        override fun onBinary(bytes: ByteArray) {
            signals.trySend(Signal.Binary(bytes))
        }

        override fun onText(text: String) {
            signals.trySend(Signal.Text(text))
        }

        override fun onClosed(reason: String?) {
            signals.trySend(Signal.Ended(reason))
            signals.close()
        }

        override fun onFailure(error: Throwable) {
            signals.trySend(Signal.Ended(error.message ?: error::class.simpleName))
            signals.close()
        }
    }

    private sealed interface Signal {
        data object Open : Signal
        class Binary(val bytes: ByteArray) : Signal
        class Text(val text: String) : Signal
        class Ended(val reason: String?) : Signal
    }
}
