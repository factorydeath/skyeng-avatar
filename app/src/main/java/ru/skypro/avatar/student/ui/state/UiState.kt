package ru.skypro.avatar.student.ui.state

import ru.skypro.avatar.student.core.network.ApiException
import ru.skypro.avatar.student.core.network.toApiException

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun httpCode(e: Throwable): String = when (val api = e.toApiException("ui", "error")) {
    is ApiException.Unauthorized -> " (сессия истекла)"
    is ApiException.Http -> " (HTTP ${api.httpCode})"
    is ApiException.Network -> " (нет связи)"
    is ApiException.Parse -> " (неожиданный ответ сервера)"
    else -> " (${api::class.simpleName ?: "ошибка"})"
}
