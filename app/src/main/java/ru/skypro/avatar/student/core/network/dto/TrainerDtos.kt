package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestPrepareRequestDto(val testId: String)

@Serializable
data class TestPrepareResponseDto(
    val nextTaskId: String? = null,
    val status: String = "",
)

@Serializable
data class TrainerTaskDto(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
)

@Serializable
data class SubmissionDto(
    val id: Long = 0L,
    val type: String = "",
    val status: String = "",
    val code: String = "",
    val createdAt: String = "",
    val score: Double? = null,
)

@Serializable
data class TaskStudentDto(
    val id: String = "",
    val submissions: List<SubmissionDto> = emptyList(),
)

@Serializable
data class FinishTaskRequestDto(
    val taskId: String,
    val completionReason: String? = null,
)

@Serializable
data class FinishTaskResponseDto(
    val status: String = "",
    val nextTaskId: String? = null,
    val transitionId: Long? = null,
)

@Serializable
data class NextTaskStatusDto(
    val status: String = "",
    val taskId: String? = null,
    val error: String? = null,
)

@Serializable
data class RunRequestDto(
    val id: String,
    val code: String,
)

@Serializable
data class RunResponseDto(val submissionId: Long = 0L)
