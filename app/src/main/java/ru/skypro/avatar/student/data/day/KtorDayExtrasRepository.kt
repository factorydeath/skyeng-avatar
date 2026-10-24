package ru.skypro.avatar.student.data.day

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.decodeOrThrow
import ru.skypro.avatar.student.core.network.dto.BrsResponseDto
import ru.skypro.avatar.student.core.network.retryApi
import ru.skypro.avatar.student.domain.day.BrsSubject
import ru.skypro.avatar.student.domain.day.BrsSummary
import ru.skypro.avatar.student.domain.day.BrsWeek

class KtorDayExtrasRepository(private val client: HttpClient) {
    fun rating(): Flow<BrsSummary?> = flow {
        AppLog.d(AppLog.DAY, "rating start")
        try {
            val dto: BrsResponseDto = retryApi(AppLog.DAY, "brs-rating") {
                client.get(ApiEndpoints.BRS_RATING)
                    .decodeOrThrow(AppLog.DAY, "brs-rating")
            }
            emit(dto.toBrsSummary())
        } catch (e: Exception) {
            AppLog.e(AppLog.DAY, "rating failed", e)
            throw e
        }
    }
}

internal fun BrsResponseDto.toBrsSummary(): BrsSummary? {
    if (availability != null && availability != "available") return null
    val r = rating ?: return null
    val total = r.currentTotal ?: throw ru.skypro.avatar.student.core.network.ApiException.Parse(IllegalArgumentException("Missing BRS total"))
    val disciplinary = r.disciplinary_points
        ?: throw ru.skypro.avatar.student.core.network.ApiException.Parse(IllegalArgumentException("Missing BRS disciplinary points"))
    return BrsSummary(
        totalEarned = total.earned, totalPlanned = total.planned, totalAvailable = total.available, percent = total.percent,
        academicEarned = r.earned, academicPlanned = r.planned,
        disciplinaryCurrent = disciplinary.current, disciplinaryInitial = disciplinary.initial,
        contribution = r.contribution_points,
        weeks = r.weekly_points.sortedBy { it.week }.map { BrsWeek(it.week, it.earned, it.available) },
        subjects = r.tracks.map { BrsSubject(it.id, it.title, it.earned, it.available) },
    )
}
