package ru.skypro.avatar.student.data.aihelper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object AiWsType {
    const val INIT = "from_client__init"
    const val SEND_TEXT = "from_client__send_text"
    const val RESET = "from_client__reset"
    const val FEEDBACK = "from_client__feedback"
    const val INIT_RESPONSE = "from_server__init_response"
    const val TEXT_RESPONSE = "from_server__text_response"
    const val DELIVERY_STATUS = "from_server__delivery_status"
    const val MESSAGE_ACK = "from_server__message_ack"
    const val CONNECTED = "from_server__connected"
    const val JWT_SUCCESS = "from_server__jwt_success"
    const val JWT_ERROR = "from_server__jwt_error"
    const val INTERNAL_ERROR = "from_server__internal_error"
}

@Serializable
data class WsEnvelope(val type: String = "", val data: JsonObject = JsonObject(emptyMap()))

@Serializable
data class InitPayload(val taskId: String, val isE2ETest: Boolean = false, val language: String = "ru")

@Serializable
data class SendTextPayload(
    val messageId: String,
    val message: String,
    val scenario: String = SCENARIO_DEFAULT,
    val scenarioContext: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class ResetPayload(val messageId: String)

@Serializable
data class FeedbackPayload(val messageId: String, val itemId: String, val like: Boolean)

@Serializable
data class HistoryItemDto(
    val itemId: String? = null,
    val message: String? = null,
    val createdAt: String? = null,
    val isFromUser: Boolean = false,
    val like: Boolean? = null,
    val replyOnMessageId: String? = null,
)

@Serializable
data class InitResponseDto(val history: List<HistoryItemDto> = emptyList())

@Serializable
data class TextResponseDto(
    val replyOnMessageId: String? = null,
    val itemId: String? = null,
    val text: String? = null,
)

@Serializable
data class DeliveryStatusDto(val replyOnMessageId: String? = null)

@Serializable
data class ServerErrorDto(
    val messageId: String? = null,
    val canRetry: Boolean = false,
    val errorKey: String? = null,
    val errorClass: String? = null,
    val errorMessage: String? = null,
)

data class AiChatMessage(
    val itemId: String?,
    val text: String,
    val isFromUser: Boolean,
    val like: Boolean?,
    val localId: String = java.util.UUID.randomUUID().toString(),
)

fun HistoryItemDto.toMessage(): AiChatMessage? {
    val text = message ?: return null
    return AiChatMessage(itemId = itemId, text = text, isFromUser = isFromUser, like = like)
}

const val SCENARIO_DEFAULT = "math_default"

val AI_JSON = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true; encodeDefaults = true }

fun parseEnvelope(raw: String): WsEnvelope = AI_JSON.decodeFromString(raw)

fun InitResponseDto.messages(): List<AiChatMessage> = history.mapNotNull { it.toMessage() }

inline fun <reified T> WsEnvelope.decodeData(): T = AI_JSON.decodeFromJsonElement(data)
