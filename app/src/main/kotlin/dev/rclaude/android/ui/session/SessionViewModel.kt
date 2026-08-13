package dev.rclaude.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rclaude.android.data.SettingsRepository
import dev.rclaude.protocol.ChatEvent
import dev.rclaude.protocol.ClientMessage
import dev.rclaude.protocol.ServerMessage
import dev.rclaude.protocol.TerminalKeys
import dev.rclaude.protocol.net.ConnectionEvent
import dev.rclaude.protocol.net.RemoteClaudeApi
import dev.rclaude.protocol.net.ServerEndpoints
import dev.rclaude.protocol.net.SessionConnection
import dev.rclaude.protocol.net.SessionSocketFactory
import dev.rclaude.protocol.term.TerminalEmulator
import dev.rclaude.protocol.term.TerminalSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние экрана сессии. */
data class SessionUiState(
    val name: String = "",
    val cwd: String = "",
    val cols: Int = 80,
    val rows: Int = 24,
    val loaded: Boolean = false,
    val connected: Boolean = false,
    val chatAvailable: Boolean = true,
    val chatNote: String? = null,
    val events: List<ChatEvent> = emptyList(),
    val menuWaiting: Boolean = false,
    val status: String? = null,
    val finished: Boolean = false,
    val snapshot: TerminalSnapshot = TerminalSnapshot.EMPTY,
)

/**
 * Живое подключение к сессии: поток вывода терминала, чат-лента, состояние меню и
 * отправка ввода. Эмулятором владеет отдельная корутина — снимок публикуется не чаще
 * раза в [SNAPSHOT_INTERVAL_MS] мс.
 */
class SessionViewModel(
    private val sessionId: String,
    private val api: RemoteClaudeApi,
    private val settings: SettingsRepository,
    private val socketFactory: SessionSocketFactory,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val commands = Channel<TerminalCommand>(Channel.UNLIMITED)
    private var connection: SessionConnection? = null

    init {
        viewModelScope.launch(Dispatchers.Default) { runTerminal() }
        viewModelScope.launch { runSession() }
    }

    /** Отправляет промпт вставкой в скобках с переводом строки. */
    fun sendPrompt(text: String) {
        if (text.isBlank()) return
        connection?.send(ClientMessage.Input(TerminalKeys.bracketedPaste(text)))
    }

    /** Отправляет готовую последовательность панели клавиш. */
    fun sendKey(sequence: String) {
        connection?.send(ClientMessage.Input(sequence))
    }

    /** Подгоняет размер PTY под экран телефона. */
    fun resize(cols: Int, rows: Int) {
        if (cols <= 0 || rows <= 0) return
        connection?.send(ClientMessage.Resize(cols, rows))
        commands.trySend(TerminalCommand.Resize(cols, rows))
        _state.update { it.copy(cols = cols, rows = rows) }
    }

    private suspend fun runSession() {
        val stored = settings.current()
        if (stored == null) {
            fail("Подключение не настроено — вернись к настройкам.")
            return
        }
        val sessions = runCatching { api.sessions(stored.address, stored.token) }
            .getOrElse {
                fail(it.message ?: "сервер недоступен")
                return
            }
        val session = sessions.firstOrNull { it.id == sessionId }
        if (session == null) {
            fail("Сессия не найдена — обнови список.")
            return
        }
        commands.trySend(TerminalCommand.Resize(session.cols, session.rows))
        _state.update {
            it.copy(
                name = session.name,
                cwd = session.cwd,
                cols = session.cols,
                rows = session.rows,
                chatAvailable = session.chat,
                chatNote = if (session.chat) null else TRANSCRIPT_NOTE,
                loaded = true,
            )
        }
        val link = SessionConnection(socketFactory)
        connection = link
        val url = ServerEndpoints.sessionSocket(stored.address, sessionId, stored.token)
        link.events(url).collect(::handleEvent)
        _state.update { current ->
            current.copy(connected = false, status = current.status ?: "Соединение закрыто.")
        }
    }

    private fun handleEvent(event: ConnectionEvent) {
        when (event) {
            ConnectionEvent.Connected -> {
                commands.trySend(TerminalCommand.Reset)
                _state.update { it.copy(connected = true, status = null) }
            }

            is ConnectionEvent.Output -> commands.trySend(TerminalCommand.Write(event.bytes))
            is ConnectionEvent.Incoming -> handleMessage(event.message)
            is ConnectionEvent.Reconnecting -> _state.update {
                it.copy(
                    connected = false,
                    status = "Связь потеряна, переподключаюсь через ${event.delayMs / 1000} с…",
                )
            }
        }
    }

    private fun handleMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.Chat -> if (message.unavailable) {
                _state.update { it.copy(chatNote = TRANSCRIPT_NOTE) }
            } else {
                _state.update { current ->
                    val merged = if (message.reset) message.events else current.events + message.events
                    current.copy(
                        chatAvailable = true,
                        chatNote = null,
                        events = merged.takeLast(CHAT_EVENTS_LIMIT),
                    )
                }
            }

            is ServerMessage.Status -> _state.update { it.copy(menuWaiting = message.menuWaiting) }
            is ServerMessage.Exit -> _state.update {
                it.copy(
                    connected = false,
                    finished = true,
                    menuWaiting = false,
                    status = "Сессия завершена (код ${message.code}).",
                )
            }

            is ServerMessage.Failure -> _state.update { it.copy(status = message.message) }
            is ServerMessage.Unknown -> Unit
        }
    }

    private fun fail(text: String) {
        _state.update { it.copy(loaded = true, status = text, finished = true) }
    }

    /** Единственный владелец эмулятора: применяет команды и публикует снимки. */
    private suspend fun runTerminal() {
        val emulator = TerminalEmulator()
        while (true) {
            var command: TerminalCommand? = commands.receive()
            while (command != null) {
                when (command) {
                    is TerminalCommand.Write -> emulator.write(command.bytes)
                    TerminalCommand.Reset -> emulator.reset()
                    is TerminalCommand.Resize -> emulator.resize(command.cols, command.rows)
                }
                command = commands.tryReceive().getOrNull()
            }
            val snapshot = emulator.snapshot()
            _state.update { it.copy(snapshot = snapshot) }
            delay(SNAPSHOT_INTERVAL_MS)
        }
    }

    private sealed interface TerminalCommand {
        class Write(val bytes: ByteArray) : TerminalCommand
        data object Reset : TerminalCommand
        class Resize(val cols: Int, val rows: Int) : TerminalCommand
    }

    private companion object {
        const val SNAPSHOT_INTERVAL_MS = 60L
        const val CHAT_EVENTS_LIMIT = 500
        const val TRANSCRIPT_NOTE =
            "Транскрипт пока не найден — лента появится, когда в сессии будет первое сообщение."
    }
}
