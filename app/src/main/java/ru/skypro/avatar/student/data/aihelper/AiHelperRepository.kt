package ru.skypro.avatar.student.data.aihelper

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import ru.skypro.avatar.student.core.network.ApiException
import ru.skypro.avatar.student.core.session.notifySessionExpired

class AiHelperException(message: String, val canRetry: Boolean = false) : Exception(message)

class AiHelperRepository(private val client: HttpClient) {
    private class Connection(parent: Job) {
        val scope = CoroutineScope(SupervisorJob(parent) + Dispatchers.IO)
        val router = AiResponseRouter()
        var socket: ClientWebSocketSession? = null
        fun close() {
            router.fail(ApiException.Network(IOException("Connection closed")))
            scope.cancel()
            socket?.cancel()
        }
    }

    private var connection: Connection? = null

    @Synchronized
    fun close() {
        connection?.close()
        connection = null
    }

    fun dispose() = close()

    @Synchronized
    private fun newConnection(): Connection {
        close()
        return Connection(client.coroutineContext.job).also { connection = it }
    }

    @Synchronized
    private fun current(): Connection = connection ?: throw AiHelperException("Помощник не подключён")

    suspend fun open(taskId: String): List<AiChatMessage> {
        val c = newConnection()
        try {
            return c.scope.async {
                val socket = client.webSocketSession(WS_URL)
                synchronized(this@AiHelperRepository) {
                    if (connection !== c) {
                        socket.cancel()
                        throw CancellationException("Obsolete connection")
                    }
                    c.socket = socket
                }
                c.scope.launch {
                    try {
                        for (frame in socket.incoming) if (frame is Frame.Text) {
                            val env = parseEnvelope(frame.readText())
                            if (env.type == AiWsType.JWT_ERROR) client.notifySessionExpired()
                            c.router.receive(env)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        c.router.fail(e)
                    } finally {
                        c.router.fail(ApiException.Network(IOException("Connection lost")))
                        socket.cancel()
                    }
                }
                c.router.awaitAuthorization(INIT_TIMEOUT_MS)
                val env = c.router.request(INIT_TIMEOUT_MS, null, { it.type == AiWsType.INIT_RESPONSE }) {
                    sendFrame(c, AiWsType.INIT, InitPayload(taskId))
                }
                env.decodeData<InitResponseDto>().messages()
            }.await()
        } catch (e: Exception) {
            synchronized(this) { if (connection === c) close() }
            if (e is TimeoutCancellationException) throw ApiException.Network(e)
            throw e
        }
    }

    suspend fun send(text: String): AiChatMessage {
        val c = current()
        val id = UUID.randomUUID().toString()
        val env = request(c, TEXT_TIMEOUT_MS, id, { e ->
            when (e.type) {
                AiWsType.TEXT_RESPONSE -> e.decodeData<TextResponseDto>().replyOnMessageId == id
                AiWsType.INIT_RESPONSE -> e.decodeData<InitResponseDto>().history.any { !it.isFromUser && it.replyOnMessageId == id }
                else -> false
            }
        }) { sendFrame(c, AiWsType.SEND_TEXT, SendTextPayload(id, text)) }
        return if (env.type == AiWsType.TEXT_RESPONSE) {
            val dto = env.decodeData<TextResponseDto>()
            AiChatMessage(dto.itemId, dto.text ?: throw AiHelperException("Пустой ответ помощника"), false, null)
        } else {
            env.decodeData<InitResponseDto>().history.first { !it.isFromUser && it.replyOnMessageId == id }
                .toMessage() ?: throw AiHelperException("Пустой ответ помощника")
        }
    }

    suspend fun reset() {
        val c = current()
        val id = UUID.randomUUID().toString()
        request(c, TEXT_TIMEOUT_MS, id, {
            it.type == AiWsType.DELIVERY_STATUS && it.decodeData<DeliveryStatusDto>().replyOnMessageId == id
        }) { sendFrame(c, AiWsType.RESET, ResetPayload(id)) }
    }

    suspend fun like(itemId: String, like: Boolean) {
        sendFrame(current(), AiWsType.FEEDBACK, FeedbackPayload(UUID.randomUUID().toString(), itemId, like))
    }

    private suspend fun request(c: Connection, timeout: Long, id: String?, match: (WsEnvelope) -> Boolean, send: suspend () -> Unit): WsEnvelope =
        try {
            c.router.request(timeout, id, match, send)
        } catch (e: TimeoutCancellationException) {
            synchronized(this) { if (connection === c) close() }
            throw ApiException.Network(e)
        }

    private suspend inline fun <reified T> sendFrame(c: Connection, type: String, payload: T) {
        val socket = c.socket ?: throw AiHelperException("Помощник не подключён")
        socket.send(AI_JSON.encodeToString(WsEnvelope(type, AI_JSON.encodeToJsonElement(serializer<T>(), payload) as JsonObject)))
    }

    private companion object {
        const val WS_URL = "wss://edu-avatar-speech-recognition-proxy.skyeng.ru/ws/math-helper"
        const val INIT_TIMEOUT_MS = 30_000L
        const val TEXT_TIMEOUT_MS = 68_000L
    }
}
