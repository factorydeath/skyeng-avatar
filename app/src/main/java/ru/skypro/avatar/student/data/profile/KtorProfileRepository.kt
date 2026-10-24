package ru.skypro.avatar.student.data.profile

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.decodeOrThrow
import ru.skypro.avatar.student.core.network.getDecoded
import ru.skypro.avatar.student.core.network.dto.AcademicSummaryDto
import ru.skypro.avatar.student.core.network.dto.HelpfulLinksDto
import ru.skypro.avatar.student.core.network.dto.QuestionnaireDto

class KtorProfileRepository(private val client: HttpClient) {
    fun summary(): Flow<AcademicSummaryDto> = flow {
        AppLog.d(AppLog.PROFILE, "summary start")
        try {
            val dto: AcademicSummaryDto = client.getDecoded(ApiEndpoints.ACADEMIC_SUMMARY, AppLog.PROFILE, "summary")
            AppLog.i(AppLog.PROFILE, "summary ok debts=${dto.debts}")
            emit(dto)
        } catch (e: Exception) {
            AppLog.e(AppLog.PROFILE, "summary failed", e)
            throw e
        }
    }

    fun questionnaire(): Flow<QuestionnaireDto> = flow {
        AppLog.d(AppLog.PROFILE, "questionnaire start")
        try {
            val dto: QuestionnaireDto = client.getDecoded(ApiEndpoints.QUESTIONNAIRE, AppLog.PROFILE, "questionnaire")
            AppLog.i(AppLog.PROFILE, "questionnaire ok")
            emit(dto)
        } catch (e: Exception) {
            AppLog.e(AppLog.PROFILE, "questionnaire failed", e)
            throw e
        }
    }

    fun helpfulLinks(): Flow<HelpfulLinksDto> = flow {
        AppLog.d(AppLog.PROFILE, "links start")
        try {
            val dto: HelpfulLinksDto = client.getDecoded(ApiEndpoints.HELPFUL_LINKS, AppLog.PROFILE, "links")
            AppLog.i(AppLog.PROFILE, "links ok")
            emit(dto)
        } catch (e: Exception) {
            AppLog.e(AppLog.PROFILE, "links failed", e)
            throw e
        }
    }
}
