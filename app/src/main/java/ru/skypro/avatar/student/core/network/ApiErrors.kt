package ru.skypro.avatar.student.core.network

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.io.IOException
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.skypro.avatar.student.core.logging.AppLog

sealed class ApiException(message: String, val httpCode: Int? = null) : Exception(message) {
    class Http(code: Int) : ApiException("HTTP $code", code)
    class Network(cause: Throwable) : ApiException(cause.message ?: "network", null)
    class Parse(cause: Throwable) : ApiException(cause.message ?: "parse", null)
    class Unauthorized : ApiException("HTTP 401", 401)
}

suspend inline fun <reified T> HttpResponse.decodeOrThrow(tag: String, what: String): T {
    val code = status.value
    val raw = bodyAsText()
    if (!status.isSuccess()) {
        AppLog.w(tag, "$what http=$code len=${raw.length}")
        if (code == 401) {
            throw ApiException.Unauthorized()
        }
        throw ApiException.Http(code)
    }
    try {
        return ApiJson.decodeFromString<T>(raw)
    } catch (e: SerializationException) {
        AppLog.e(tag, "$what parse failed len=${raw.length}", e)
        throw ApiException.Parse(e)
    } catch (e: IllegalArgumentException) {
        AppLog.e(tag, "$what parse failed len=${raw.length}", e)
        throw ApiException.Parse(e)
    }
}

val ApiJson = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

suspend fun <T> retryApi(tag: String, what: String, attempts: Int = 3, block: suspend () -> T): T {
    require(attempts in 1..3)
    var attempt = 1
    while (true) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException.Unauthorized) {
            throw e
        } catch (e: Exception) {
            if (attempt >= attempts || !e.isTransientReadFailure()) throw e
            AppLog.w(tag, "$what attempt=$attempt failed, повтор", e)
            delay(RETRY_DELAY_MS * attempt)
            attempt++
        }
    }
}

internal fun Throwable.isTransientReadFailure(): Boolean = when (this) {
    is CancellationException -> false
    is ApiException.Http -> httpCode in setOf(408, 429, 500, 502, 503, 504)
    is ResponseException -> response.status.value in setOf(408, 429, 500, 502, 503, 504)
    is ApiException.Network, is IOException, is HttpRequestTimeoutException -> true
    else -> false
}

suspend inline fun <reified T> HttpClient.getDecoded(
    url: String,
    tag: String,
    what: String,
    crossinline configure: HttpRequestBuilder.() -> Unit = {},
): T = retryApi(tag, what) { get(url) { configure() }.decodeOrThrow<T>(tag, what) }

private const val RETRY_DELAY_MS = 1_000L

fun Throwable.toApiException(tag: String, what: String): Throwable = when (this) {
    is ApiException -> this
    is CancellationException -> this
    is SerializationException -> ApiException.Parse(this)
    is IOException -> ApiException.Network(this)
    is ResponseException -> ApiException.Http(response.status.value)
    else -> {
        AppLog.e(tag, "$what unexpected", this)
        this
    }
}

fun Throwable.userMessage(action: String): String = when (this) {
    is ApiException.Unauthorized -> "$action: сессия истекла, войди заново"
    is ApiException.Http -> "$action (HTTP ${httpCode})"
    is ApiException.Network -> "$action: нет связи с сервером"
    is ApiException.Parse -> "$action: сервер вернул неожиданный ответ"
    else -> "$action: что-то пошло не так"
}
