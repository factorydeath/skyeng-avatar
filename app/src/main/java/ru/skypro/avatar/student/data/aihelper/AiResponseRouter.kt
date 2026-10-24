package ru.skypro.avatar.student.data.aihelper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import ru.skypro.avatar.student.core.network.ApiException

internal class AiResponseRouter {
    private data class Pending(val id: String?, val match: (WsEnvelope) -> Boolean, val result: CompletableDeferred<WsEnvelope>)
    private val pending = mutableListOf<Pending>()
    private val authorized = CompletableDeferred<Unit>()
    private var failure: Throwable? = null

    suspend fun awaitAuthorization(timeout: Long) = withTimeout(timeout) { authorized.await() }

    suspend fun request(timeout: Long, id: String?, match: (WsEnvelope) -> Boolean, send: suspend () -> Unit): WsEnvelope {
        val entry = Pending(id, match, CompletableDeferred())
        synchronized(this) {
            failure?.let { throw it }
            pending += entry
        }
        try {
            return withTimeout(timeout) {
                send()
                entry.result.await()
            }
        } finally {
            synchronized(this) { pending.remove(entry) }
            entry.result.cancel()
        }
    }

    @Synchronized
    fun receive(env: WsEnvelope) {
        if (failure != null) return
        when (env.type) {
            AiWsType.JWT_SUCCESS -> authorized.complete(Unit)
            AiWsType.JWT_ERROR -> fail(ApiException.Unauthorized())
            else -> pending.toList().forEach { entry ->
                try {
                    if (env.type == AiWsType.INTERNAL_ERROR) {
                        val error = env.decodeData<ServerErrorDto>()
                        if (error.messageId == null || error.messageId == entry.id) {
                            entry.result.completeExceptionally(AiHelperException("Ошибка помощника", error.canRetry))
                        }
                    } else if (entry.match(env)) entry.result.complete(env)
                } catch (e: Exception) {
                    entry.result.completeExceptionally(e)
                }
            }
        }
    }

    @Synchronized
    fun fail(cause: Throwable) {
        if (failure != null) return
        failure = cause
        authorized.completeExceptionally(cause)
        pending.forEach { it.result.completeExceptionally(cause) }
        pending.clear()
    }
}
