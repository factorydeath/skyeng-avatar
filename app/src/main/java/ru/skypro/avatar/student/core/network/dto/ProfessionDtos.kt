package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfessionHomeworkDto(
    val has: Boolean = false,
    val homeworkDeadline: String? = null,
    val homeWorkUrl: String? = null,
    val isHomeworkCompleted: Boolean = false,
    val status: String? = null,
    val isDebt: Boolean = false,
)

@Serializable
data class ProfessionLessonDto(
    val id: Long = 0L,
    val streamLessonTitle: String = "",
    val streamLessonLink: String? = null,
    val launchUrl: String? = null,
    val startAt: String = "",
    val deadline: String? = null,
    val completeness: Int = 0,
    val score: Int? = null,
    val lessonType: String = "",
    val homework: ProfessionHomeworkDto? = null,
)

@Serializable
data class ProfessionLiveDto(
    val id: Long = 0L,
    val title: String = "",
    val startAt: String = "",
    val endAt: String = "",
    val joinUrl: String? = null,
    val trackingJoinUrl: String? = null,
    val recordingUrl: String? = null,
    val recordingExpected: Boolean = false,
)

@Serializable
data class ProfessionModuleDto(
    val moduleTitle: String = "",
    val moduleId: Long = 0L,
    val firstEventDate: String = "",
    val lessons: List<ProfessionLessonDto> = emptyList(),
    val lives: List<ProfessionLiveDto> = emptyList(),
    val isComplete: Boolean = false,
)

@Serializable
data class ProfessionDetailDto(
    val programId: Long = 0L,
    val programTitle: String = "",
    val modules: List<ProfessionModuleDto> = emptyList(),
)
