package ru.skypro.avatar.student.data.trainer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.ApiException
import ru.skypro.avatar.student.core.network.decodeOrThrow
import ru.skypro.avatar.student.core.network.retryApi
import ru.skypro.avatar.student.core.network.dto.FinishTaskRequestDto
import ru.skypro.avatar.student.core.network.dto.FinishTaskResponseDto
import ru.skypro.avatar.student.core.network.dto.NextTaskStatusDto
import ru.skypro.avatar.student.core.network.dto.RunRequestDto
import ru.skypro.avatar.student.core.network.dto.RunResponseDto
import ru.skypro.avatar.student.core.network.dto.TaskStudentDto
import ru.skypro.avatar.student.core.network.dto.TestPrepareRequestDto
import ru.skypro.avatar.student.core.network.dto.TestPrepareResponseDto
import ru.skypro.avatar.student.core.network.dto.TrainerTaskDto
import ru.skypro.avatar.student.domain.trainer.NextTaskResult
import ru.skypro.avatar.student.domain.trainer.Submission
import ru.skypro.avatar.student.domain.trainer.TrainerTask
import ru.skypro.avatar.student.domain.trainer.TrainerVerdict
import ru.skypro.avatar.student.domain.trainer.trainerVerdict

class KtorTrainerRepository(private val client: HttpClient) {
    suspend fun prepare(testId: String): NextTaskResult {
        AppLog.d(AppLog.TRAINER, "test-prepare")
        val dto: TestPrepareResponseDto = client.post("${ApiEndpoints.CODE}/test-prepare") {
            contentType(ContentType.Application.Json)
            setBody(TestPrepareRequestDto(testId))
        }.decodeOrThrow(AppLog.TRAINER, "test-prepare")
        return when (dto.status) {
            "ok" -> NextTaskResult.Found(dto.nextTaskId ?: throw ApiException.Parse(IllegalStateException("nextTaskId")))
            "all_tasks_finished" -> NextTaskResult.NoTasksLeft
            else -> throw ApiException.Parse(IllegalStateException("Unexpected prepare status"))
        }
    }

    suspend fun finishTask(taskId: String, skipConfirmed: Boolean): NextTaskResult {
        AppLog.d(AppLog.TRAINER, "finish-task skip=$skipConfirmed")
        val response = client.post("${ApiEndpoints.CODE}/finish-task") {
            contentType(ContentType.Application.Json)
            setBody(FinishTaskRequestDto(taskId, if (skipConfirmed) SKIP_REASON else null))
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            AppLog.w(AppLog.TRAINER, "finish-task http=${response.status.value} len=${text.length}")
            if (response.status.value == 401) {
                throw ApiException.Unauthorized()
            }
            if (text.contains(SKIP_REQUIRED_REASON)) return NextTaskResult.SkipConfirmationRequired
            throw ApiException.Http(response.status.value)
        }
        val dto = try {
            JSON.decodeFromString<FinishTaskResponseDto>(text)
        } catch (e: SerializationException) {
            AppLog.e(AppLog.TRAINER, "finish-task parse failed len=${text.length}", e)
            throw ApiException.Parse(e)
        }
        return when (dto.status) {
            "ok" -> NextTaskResult.Found(dto.nextTaskId ?: throw ApiException.Parse(IllegalStateException("nextTaskId")))
            "calculation_in_progress" -> {
                val transitionId = dto.transitionId ?: throw ApiException.Parse(IllegalStateException("transitionId"))
                NextTaskResult.Pending(transitionId.toString())
            }
            "all_tasks_finished" -> NextTaskResult.NoTasksLeft
            else -> throw ApiException.Parse(IllegalStateException("status=${dto.status}"))
        }
    }

    suspend fun pollNextTask(transitionId: String): NextTaskResult {
        repeat(MAX_POLLS) {
            val dto: NextTaskStatusDto = retryApi(AppLog.TRAINER, "next-task-status") {
                client.get("${ApiEndpoints.CODE}/next-task-status/$transitionId")
                    .decodeOrThrow(AppLog.TRAINER, "next-task-status")
            }
            when (dto.status) {
                "finished" -> return NextTaskResult.Found(
                    dto.taskId ?: throw ApiException.Parse(IllegalStateException("taskId")),
                )
                "all_tasks_finished" -> return NextTaskResult.NoTasksLeft
                "error" -> throw ApiException.Parse(IllegalStateException(dto.error ?: "next-task error"))
            }
            delay(POLL_DELAY_MS)
        }
        throw ApiException.Parse(IllegalStateException("next-task polling timeout"))
    }

    suspend fun task(taskId: String): TrainerTask {
        AppLog.d(AppLog.TRAINER, "get-task")
        val dto: TrainerTaskDto = retryApi(AppLog.TRAINER, "get-task") {
            client.get("${ApiEndpoints.CODE}/get-task/$taskId")
                .decodeOrThrow(AppLog.TRAINER, "get-task")
        }
        return TrainerTask(dto.id, dto.title, parseCondition(dto.description))
    }

    suspend fun image(url: String): ByteArray? = try {
        retryApi(AppLog.TRAINER, "image") {
            client.prepareGet(ApiEndpoints.absolute(url)).execute { response ->
                if (!response.status.isSuccess()) throw ApiException.Http(response.status.value)
                readImageBytes(response.bodyAsChannel(), response.contentLength())
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.w(AppLog.TRAINER, "image load failed", e)
        null
    }

    suspend fun submit(taskId: String, answer: String): Long {
        AppLog.d(AppLog.TRAINER, "run")
        val dto: RunResponseDto = client.post("${ApiEndpoints.CODE}/run") {
            contentType(ContentType.Application.Json)
            setBody(RunRequestDto(taskId, answer))
        }.decodeOrThrow(AppLog.TRAINER, "run")
        return dto.submissionId
    }

    suspend fun verdict(taskId: String, submissionId: Long): TrainerVerdict {
        repeat(MAX_POLLS) {
            val dto: TaskStudentDto = retryApi(AppLog.TRAINER, "get-task-student") {
                client.get("${ApiEndpoints.CODE}/get-task-student/$taskId")
                    .decodeOrThrow(AppLog.TRAINER, "get-task-student")
            }
            val mine = dto.submissions
                .filter { it.id == submissionId }
                .map { Submission(it.id, it.status, it.score, it.createdAt) }
            val verdict = trainerVerdict(mine)
            if (verdict !is TrainerVerdict.Default && verdict !is TrainerVerdict.Processing) return verdict
            delay(POLL_DELAY_MS)
        }
        return TrainerVerdict.Processing(submissionId)
    }

    private companion object {
        const val MAX_POLLS = 15
        const val POLL_DELAY_MS = 1_000L
        val JSON = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        const val SKIP_REASON = "skipConfirmed"
        const val SKIP_REQUIRED_REASON = "skipConfirmationRequired"
    }
}
