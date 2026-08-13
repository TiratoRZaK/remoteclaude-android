package dev.rclaude.protocol

import kotlinx.serialization.Serializable

/** Ответ `GET /api/health`. */
@Serializable
data class ServerHealth(val ok: Boolean = false, val version: String? = null)

/** Элемент списка `GET /api/sessions`. */
@Serializable
data class SessionInfo(
    val id: String,
    val name: String = "",
    val cwd: String = "",
    val cols: Int = 80,
    val rows: Int = 24,
    val startedAt: String? = null,
    val viewers: Int = 0,
    val chat: Boolean = false,
)

/** Вид записи в чат-ленте. */
enum class ChatKind(val wire: String) {
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    ;

    companion object {
        fun fromWire(value: String?): ChatKind? = entries.firstOrNull { it.wire == value }
    }
}

/** Запись чат-ленты: реплика пользователя, ответ Claude или вызов инструмента. */
data class ChatEvent(
    val kind: ChatKind,
    val text: String? = null,
    val name: String? = null,
    val detail: String? = null,
    val ts: String? = null,
) {
    /** Строка для показа: текст реплики либо «инструмент · деталь». */
    val display: String
        get() = when (kind) {
            ChatKind.TOOL -> listOfNotNull(name, detail?.takeIf { it.isNotBlank() }).joinToString(" · ")
            else -> text.orEmpty()
        }
}

/** Текстовый кадр сервера. */
sealed interface ServerMessage {

    /** Порция чат-ленты; [reset] — заменить ленту целиком, [unavailable] — транскрипт не найден. */
    data class Chat(
        val events: List<ChatEvent> = emptyList(),
        val reset: Boolean = false,
        val unavailable: Boolean = false,
    ) : ServerMessage

    /** Состояние детектора меню: Claude ждёт ответа в терминале. */
    data class Status(val menuWaiting: Boolean) : ServerMessage

    /** Процесс claude завершился. */
    data class Exit(val code: Int) : ServerMessage

    /** Ошибка уровня сессии. */
    data class Failure(val message: String) : ServerMessage

    /** Кадр незнакомого типа — протокол расширился, клиент его игнорирует. */
    data class Unknown(val type: String) : ServerMessage
}

/** Текстовый кадр клиента. */
sealed interface ClientMessage {

    /** Ввод в PTY: текст или escape-последовательность. */
    data class Input(val data: String) : ClientMessage

    /** Смена размера PTY. */
    data class Resize(val cols: Int, val rows: Int) : ClientMessage
}
