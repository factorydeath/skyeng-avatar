package ru.skypro.avatar.student.ui.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.data.aihelper.AiChatMessage
import ru.skypro.avatar.student.data.aihelper.AiHelperRepository
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode

data class AiHelperContent(
    val taskId: String = "",
    val messages: List<AiChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val reconnectRequired: Boolean = false,
)

class AiHelperViewModel(private val repo: AiHelperRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<AiHelperContent>>(UiState.Loading)
    val state: StateFlow<UiState<AiHelperContent>> = _state
    private val _openTask = MutableStateFlow<String?>(null)
    val openTask: StateFlow<String?> = _openTask
    private var operations = operationScope()

    private fun operationScope() = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job))

    fun open(taskId: String) {
        if (_openTask.value == taskId && _state.value !is UiState.Error) return
        close()
        _openTask.value = taskId
        AppLog.d(AppLog.TRAINER, "ai vm open")
        _state.value = UiState.Loading
        operations.launch {
            try {
                val history = repo.open(taskId)
                _state.value = UiState.Content(AiHelperContent(taskId = taskId, messages = history))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "ai vm open error", e)
                _state.value = UiState.Error("Не удалось открыть помощника${httpCode(e)}")
            }
        }
    }

    fun onInput(value: String) = update { it.copy(input = value) }

    fun send() {
        val current = (_state.value as? UiState.Content)?.data ?: return
        val text = current.input.trim()
        if (text.isEmpty() || current.sending || current.reconnectRequired) return
        update { it.copy(sending = true, input = "") }
        val optimistic = current.messages + AiChatMessage(itemId = null, text = text, isFromUser = true, like = null)
        update { it.copy(messages = optimistic) }
        operations.launch {
            try {
                val answer = repo.send(text)
                update { it.copy(sending = false, messages = it.messages + answer) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "ai vm send error", e)
                update {
                    it.copy(
                        sending = false,
                        reconnectRequired = true,
                        messages = it.messages + AiChatMessage(
                            itemId = null,
                            text = "Ответ не получен${httpCode(e)}. Переподключись и проверь историю перед повторной отправкой.",
                            isFromUser = false,
                            like = null,
                        ),
                    )
                }
            }
        }
    }

    fun reset() {
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (current.sending || current.reconnectRequired) return
        update { it.copy(sending = true) }
        operations.launch {
            try {
                repo.reset()
                val history = repo.open(current.taskId)
                _state.value = UiState.Content(AiHelperContent(taskId = current.taskId, messages = history))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "ai vm reset error", e)
                _state.value = UiState.Error("Не удалось начать новый диалог${httpCode(e)}. Открой помощника заново.")
            }
        }
    }

    fun like(itemId: String?, like: Boolean) {
        if (itemId == null) return
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (current.sending || current.reconnectRequired) return
        val previous = current.messages.firstOrNull { it.itemId == itemId }?.like
        update {
            it.copy(messages = it.messages.map { m -> if (m.itemId == itemId) m.copy(like = like) else m })
        }
        operations.launch {
            try {
                repo.like(itemId, like)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "ai vm like error", e)
                update {
                    it.copy(messages = it.messages.map { m -> if (m.itemId == itemId) m.copy(like = previous) else m })
                }
            }
        }
    }

    fun retry(taskId: String) {
        close()
        open(taskId)
    }

    fun close() {
        operations.cancel()
        repo.close()
        _openTask.value = null
        _state.value = UiState.Loading
        operations = operationScope()
    }

    private fun update(transform: (AiHelperContent) -> AiHelperContent) {
        val content = (_state.value as? UiState.Content)?.data ?: return
        _state.value = UiState.Content(transform(content))
    }

    override fun onCleared() {
        repo.dispose()
    }
}
