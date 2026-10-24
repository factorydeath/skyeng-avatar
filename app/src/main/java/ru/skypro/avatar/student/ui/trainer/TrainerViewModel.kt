package ru.skypro.avatar.student.ui.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.data.trainer.KtorTrainerRepository
import ru.skypro.avatar.student.domain.trainer.NextTaskResult
import ru.skypro.avatar.student.domain.trainer.TrainerTask
import ru.skypro.avatar.student.domain.trainer.TrainerVerdict
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode

data class TrainerContent(
    val task: TrainerTask? = null,
    val answer: String = "",
    val checking: Boolean = false,
    val result: TrainerResult? = null,
    val moving: Boolean = false,
    val skipConfirmVisible: Boolean = false,
    val finished: Boolean = false,
    val submissionId: Long? = null,
    val pendingNext: NextTaskResult? = null,
)

sealed interface TrainerResult {
    data object Correct : TrainerResult
    data object Incorrect : TrainerResult
    data class Unavailable(val message: String) : TrainerResult
}

class TrainerViewModel(
    private val repo: KtorTrainerRepository,
    private val testId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<TrainerContent>>(UiState.Loading)
    val state: StateFlow<UiState<TrainerContent>> = _state
    private var started = false
    private var starting = false
    private var prepared: NextTaskResult? = null

    fun start() {
        if (started) return
        started = true
        retryStart()
    }

    fun retryStart() {
        if (starting) return
        starting = true
        AppLog.d(AppLog.TRAINER, "vm start")
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val next = prepared ?: repo.prepare(testId).also { prepared = it }
                _state.value = when (next) {
                    is NextTaskResult.Found -> UiState.Content(TrainerContent(task = repo.task(next.taskId)))
                    NextTaskResult.NoTasksLeft -> UiState.Content(TrainerContent(finished = true))
                    else -> throw IllegalStateException("Unexpected prepare result")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "vm start error", e)
                _state.value = UiState.Error("Не удалось открыть тренажёр${httpCode(e)}")
            } finally {
                starting = false
            }
        }
    }

    fun onAnswerChange(value: String) = update {
        if (it.checking || it.moving || it.pendingNext != null) it
        else if (it.submissionId != null && it.result !is TrainerResult.Unavailable) it
        else it.copy(answer = value, result = null, submissionId = null)
    }

    fun check() {
        val current = (_state.value as? UiState.Content)?.data ?: return
        val task = current.task ?: return
        val answer = current.answer.trim()
        if (answer.isEmpty() || current.checking || current.moving || current.pendingNext != null) return
        update { it.copy(checking = true, result = null) }
        viewModelScope.launch {
            val result = try {
                val submissionId = current.submissionId ?: repo.submit(task.id, answer).also { id ->
                    update { it.copy(submissionId = id) }
                }
                when (repo.verdict(task.id, submissionId)) {
                    is TrainerVerdict.Success -> TrainerResult.Correct
                    is TrainerVerdict.Partial, is TrainerVerdict.Error -> TrainerResult.Incorrect
                    else -> TrainerResult.Unavailable("Проверка не завершилась, попробуй ещё раз")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "vm check error", e)
                TrainerResult.Unavailable("Не удалось проверить ответ${httpCode(e)}")
            }
            update { it.copy(checking = false, result = result, submissionId = if (result is TrainerResult.Unavailable) it.submissionId else null) }
        }
    }

    fun next() = moveToNext(skipConfirmed = false)

    fun requestSkip() = update {
        if (it.checking || it.moving || it.pendingNext != null) it
        else if (it.submissionId != null && it.result !is TrainerResult.Unavailable) it
        else it.copy(skipConfirmVisible = true)
    }

    fun dismissSkip() = update { it.copy(skipConfirmVisible = false) }

    fun confirmSkip() {
        update { it.copy(skipConfirmVisible = false) }
        moveToNext(skipConfirmed = true)
    }

    private fun moveToNext(skipConfirmed: Boolean) {
        val current = (_state.value as? UiState.Content)?.data ?: return
        val task = current.task ?: return
        if (current.moving || current.checking) return
        if (current.submissionId != null && current.result !is TrainerResult.Unavailable) return
        update { it.copy(moving = true, result = null, submissionId = null) }
        viewModelScope.launch {
            try {
                var result = current.pendingNext ?: repo.finishTask(task.id, skipConfirmed).also { next ->
                    update { it.copy(pendingNext = next) }
                }
                if (result is NextTaskResult.Pending) {
                    result = repo.pollNextTask(result.transitionId).also { next -> update { it.copy(pendingNext = next) } }
                }
                when (result) {
                    is NextTaskResult.Found -> {
                        val next = repo.task(result.taskId)
                        _state.value = UiState.Content(TrainerContent(task = next))
                    }
                    NextTaskResult.NoTasksLeft -> _state.value = UiState.Content(TrainerContent(finished = true))
                    NextTaskResult.SkipConfirmationRequired -> update {
                        it.copy(moving = false, skipConfirmVisible = true, pendingNext = null)
                    }
                    is NextTaskResult.Pending -> error("Unexpected pending result")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.TRAINER, "vm next error", e)
                update {
                    it.copy(
                        moving = false,
                        result = TrainerResult.Unavailable("Не удалось перейти к следующей задаче${httpCode(e)}"),
                    )
                }
            }
        }
    }

    private fun update(transform: (TrainerContent) -> TrainerContent) {
        _state.update { current ->
            val content = (current as? UiState.Content)?.data ?: return@update current
            UiState.Content(transform(content))
        }
    }
}
