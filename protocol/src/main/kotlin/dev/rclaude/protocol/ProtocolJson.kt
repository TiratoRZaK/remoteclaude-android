package dev.rclaude.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull

/**
 * Разбор и сборка кадров протокола. Разбор терпимый: незнакомые поля и типы кадров
 * не роняют клиент, битый JSON даёт `null`, испорченное чат-событие выбрасывается,
 * остальные события кадра сохраняются.
 */
object ProtocolJson {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
    }

    /** Разбирает текстовый кадр сервера; `null` — кадр не JSON-объект. */
    fun parseServerMessage(text: String): ServerMessage? {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return null
        return when (val type = root.string("type")) {
            "chat" -> ServerMessage.Chat(
                events = (root["events"] as? JsonArray).orEmpty().mapNotNull { parseChatEvent(it as? JsonObject) },
                reset = root.bool("reset") ?: false,
                unavailable = root.bool("unavailable") ?: false,
            )

            "status" -> ServerMessage.Status(menuWaiting = root.bool("menuWaiting") ?: false)
            "exit" -> ServerMessage.Exit(code = root.int("code") ?: 0)
            "error" -> ServerMessage.Failure(message = root.string("message") ?: "ошибка сессии")
            null -> null
            else -> ServerMessage.Unknown(type)
        }
    }

    /** Разбирает чат-событие; `null` — незнакомый `kind` или пустая нагрузка. */
    fun parseChatEvent(obj: JsonObject?): ChatEvent? {
        if (obj == null) return null
        val kind = ChatKind.fromWire(obj.string("kind")) ?: return null
        val event = ChatEvent(
            kind = kind,
            text = obj.string("text"),
            name = obj.string("name"),
            detail = obj.string("detail"),
            ts = obj.string("ts"),
        )
        return if (event.display.isBlank()) null else event
    }

    /** Сериализует кадр клиента. */
    fun encode(message: ClientMessage): String = when (message) {
        is ClientMessage.Input -> buildJsonObject {
            put("type", JsonPrimitive("input"))
            put("data", JsonPrimitive(message.data))
        }

        is ClientMessage.Resize -> buildJsonObject {
            put("type", JsonPrimitive("resize"))
            put("cols", JsonPrimitive(message.cols))
            put("rows", JsonPrimitive(message.rows))
        }
    }.toString()

    /** Разбирает тело `GET /api/sessions`. */
    fun parseSessions(body: String): List<SessionInfo> {
        val array = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            if (obj.string("id").isNullOrEmpty()) return@mapNotNull null
            try {
                json.decodeFromJsonElement(SessionInfo.serializer(), obj)
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Разбирает тело `GET /api/health`. */
    fun parseHealth(body: String): ServerHealth = json.decodeFromString(ServerHealth.serializer(), body)

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
}
