package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class GradeDto(val value: Double? = null, val level: String = "")

@Serializable
data class ProgressDto(val completed: Int = 0, val total: Int = 0)

@Serializable
data class SubjectDebtsDto(val count: Int = 0, val rating: String = "")

@Serializable
data class SubjectDto(
    val subjectCode: String = "",
    val routeName: String = "",
    val priority: String = "",
    val grade: GradeDto? = null,
    val lessonProgress: ProgressDto? = null,
    val testProgress: ProgressDto? = null,
    val practiceProgress: ProgressDto? = null,
    val debts: SubjectDebtsDto? = null,
    val availableFrom: String = "",
    val isBlocked: Boolean = false,
)

@Serializable
data class SubjectsResponseDto(val subjects: List<SubjectDto> = emptyList())
