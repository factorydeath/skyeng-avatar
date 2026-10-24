package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PeriodDto(val start: String = "", val end: String = "")

@Serializable
data class LunchDto(val start: String = "", val end: String = "")

@Serializable
data class SubjectPageDto(
    val routeName: String = "",
    val subjectCode: String = "",
)

@Serializable
data class SlotActionDto(
    val kind: String = "",
    val title: String = "",
    val url: String? = null,
    val locked: Boolean = false,
    val openTarget: DebtOpenTargetDto? = null,
)

@Serializable
data class SlotWebinarDto(
    val joinUrl: String? = null,
    val recordingUrl: String? = null,
)

@Serializable
data class SlotTestDto(
    val joinUrl: String? = null,
    val phase: String? = null,
)

@Serializable
data class SlotExpectedDto(val title: String? = null)

@Serializable
data class SlotContextDto(
    val state: String? = null,
    val debtsCount: Int = 0,
    val expectedLesson: SlotExpectedDto? = null,
    val actions: List<SlotActionDto> = emptyList(),
    val webinar: SlotWebinarDto? = null,
    val test: SlotTestDto? = null,
)

@Serializable
data class SlotDto(
    val slotId: Long = 0L,
    val activityType: String = "",
    val subject: String? = null,
    val title: String? = null,
    val ownTitle: String? = null,
    val startsAt: String = "",
    val endsAt: String = "",
    val subjectPage: SubjectPageDto? = null,
    val context: SlotContextDto? = null,
)

@Serializable
data class DayDto(
    val date: String = "",
    val slots: List<SlotDto> = emptyList(),
)

@Serializable
data class WeekScheduleDto(
    val timezone: String = "",
    val period: PeriodDto? = null,
    val lunch: LunchDto? = null,
    val days: List<DayDto> = emptyList(),
)

@Serializable
data class DayScheduleDto(
    val enrolled: Boolean = false,
    val date: String = "",
    val timezone: String = "",
    val period: PeriodDto? = null,
    val lunch: LunchDto? = null,
    val slots: List<SlotDto> = emptyList(),
)
