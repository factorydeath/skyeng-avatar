package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SchoolLessonDto(
    val title: String = "",
    val type: String = "",
    val status: String? = null,
    val score: Double? = null,
    val taskUrl: String? = null,
    val recordUrl: String? = null,
    val deadlineAt: String? = null,
    val completedAt: String? = null,
    val passed: Boolean? = null,
    val attended: Boolean? = null,
    val watchedRecordCredited: Boolean? = null,
    val joinedAt: String? = null,
)

@Serializable
data class SchoolModuleDto(
    val title: String = "",
    val lessons: List<SchoolLessonDto> = emptyList(),
)

@Serializable
data class SchoolScheduleDto(
    val open: List<SchoolModuleDto> = emptyList(),
)

@Serializable
data class SchoolSubjectDto(
    val schedule: SchoolScheduleDto? = null,
)

@Serializable
data class EnglishNextDto(
    val title: String = "",
    val taskUrl: String? = null,
)

@Serializable
data class EnglishDto(
    val schedule: SchoolScheduleDto? = null,
    val nextLesson: EnglishNextDto? = null,
)

@Serializable
data class MathWebinarDto(
    val title: String = "",
    val taskUrl: String? = null,
    val recordUrl: String? = null,
)

@Serializable
data class MathEventDto(
    val title: String = "",
    val status: String? = null,
)

@Serializable
data class MathDayDto(
    val date: String = "",
    val events: List<MathEventDto> = emptyList(),
)

@Serializable
data class MathDetailDto(
    val webinars: List<MathWebinarDto> = emptyList(),
    val eventDays: List<MathDayDto> = emptyList(),
)
