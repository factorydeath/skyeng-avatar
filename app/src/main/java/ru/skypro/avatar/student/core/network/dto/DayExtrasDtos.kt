package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class DebtOpenTargetDto(val type: String = "", val url: String = "")

@Serializable
data class BrsTotalDto(
    val earned: Double = 0.0,
    val available: Double = 0.0,
    val planned: Double = 0.0,
    val percent: Int = 0,
)

@Serializable
data class BrsDisciplinaryDto(
    val current: Int = 0,
    val initial: Int = 0,
)

@Serializable
data class BrsWeekDto(
    val week: Int = 0,
    val earned: Double = 0.0,
    val available: Double = 0.0,
)

@Serializable
data class BrsTrackDto(
    val id: String = "",
    val title: String = "",
    val earned: Double = 0.0,
    val available: Double = 0.0,
    val planned: Double = 0.0,
)

@Serializable
data class BrsRatingDto(
    val currentTotal: BrsTotalDto? = null,
    val earned: Double = 0.0,
    val planned: Double = 0.0,
    val contribution_points: Int = 0,
    val disciplinary_points: BrsDisciplinaryDto? = null,
    val weekly_points: List<BrsWeekDto> = emptyList(),
    val status: String = "",
    val status_text: String = "",
    val tracks: List<BrsTrackDto> = emptyList(),
)

@Serializable
data class BrsResponseDto(val rating: BrsRatingDto? = null, val availability: String? = null)

@Serializable
data class LessonsSummaryDto(val completed: Int = 0, val total: Int = 0)

@Serializable
data class AcademicSummaryDto(
    val averageGrade: Double = 0.0,
    val debts: Int = 0,
    val lessons: LessonsSummaryDto? = null,
    val subjects: Int = 0,
)

@Serializable
data class QuestionnaireDataDto(
    val selectedInterests: List<String> = emptyList(),
    val favoriteInterests: List<String> = emptyList(),
    val goal: String? = null,
    val format: String? = null,
    val challenge: String? = null,
)

@Serializable
data class QuestionnaireDto(
    val version: Int = 0,
    val data: QuestionnaireDataDto? = null,
)

@Serializable
data class PersonalRouteDto(
    val title: String = "",
    val url: String = "",
)

@Serializable
data class HelpfulLinksDto(
    val personalRoute: PersonalRouteDto? = null,
)
