package ru.skypro.avatar.student.data.subjects

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.getDecoded
import ru.skypro.avatar.student.core.time.studyClock
import ru.skypro.avatar.student.core.network.dto.EnglishDto
import ru.skypro.avatar.student.core.network.dto.MathDetailDto
import ru.skypro.avatar.student.core.network.dto.ProfessionDetailDto
import ru.skypro.avatar.student.core.network.dto.SchoolSubjectDto
import ru.skypro.avatar.student.core.network.dto.SubjectsResponseDto
import ru.skypro.avatar.student.domain.subjects.DetailItem
import ru.skypro.avatar.student.domain.subjects.DetailSection
import ru.skypro.avatar.student.domain.subjects.Subject

class KtorSubjectsRepository(private val client: HttpClient) : SubjectsRepository {
    override fun subjects(): Flow<List<Subject>> = flow {
        AppLog.d(AppLog.SUBJECTS, "subjects start")
        try {
            val dto: SubjectsResponseDto = client.getDecoded(ApiEndpoints.SUBJECTS, AppLog.SUBJECTS, "subjects")
            AppLog.i(AppLog.SUBJECTS, "subjects ok n=${dto.subjects.size}")
            emit(dto.subjects.map { it.toDomain() })
        } catch (e: Exception) {
            AppLog.e(AppLog.SUBJECTS, "subjects failed", e)
            throw e
        }
    }

    override fun detailSections(subjectEnum: String): Flow<List<DetailSection>> = flow {
        AppLog.d(AppLog.SUBJECTS, "detailSections $subjectEnum kind=${detailKind(subjectEnum)}")
        val sections = try {
            when (detailKind(subjectEnum)) {
                DetailKind.PROFESSION -> {
                    val dto: ProfessionDetailDto = client.getDecoded(ApiEndpoints.SUBJECT_DETAIL, AppLog.SUBJECTS, "detail-profession") {
                        parameter("subjectEnum", subjectEnum)
                    }
                    dto.modules.map { m ->
                        DetailSection(
                            m.moduleTitle,
                            m.lives.map {
                                DetailItem(it.title, "", it.trackingJoinUrl ?: it.joinUrl ?: "")
                            } + m.lessons.map {
                                DetailItem(
                                    it.streamLessonTitle,
                                    "${it.completeness}%${if (it.homework?.isDebt == true) " · долг" else ""}",
                                    it.streamLessonLink ?: it.launchUrl ?: it.homework?.homeWorkUrl ?: "",
                                    it.homework?.isDebt == true,
                                )
                            },
                        )
                    }
                }
                DetailKind.SCHOOL -> {
                    val dto: SchoolSubjectDto = client.getDecoded(ApiEndpoints.SCHOOL_SUBJECT, AppLog.SUBJECTS, "detail-school") {
                        parameter("subjectEnum", subjectEnum)
                    }
                    val now = client.studyClock().today().toString()
                    (dto.schedule?.open ?: emptyList()).map { m ->
                        DetailSection(
                            m.title,
                            m.lessons.map {
                                DetailItem(
                                    it.title,
                                    lessonStatus(it.status, it.score),
                                    it.taskUrl ?: it.recordUrl ?: "",
                                    isLessonDebt(it.type, it.title, it.status, it.passed, it.completedAt, it.attended, it.watchedRecordCredited, it.deadlineAt, now),
                                )
                            },
                        )
                    }
                }
                DetailKind.ENGLISH -> {
                    val dto: EnglishDto = client.getDecoded(ApiEndpoints.ENGLISH_DETAIL, AppLog.SUBJECTS, "detail-english")
                    val now = client.studyClock().today().toString()
                    (dto.schedule?.open ?: emptyList()).map { m ->
                        DetailSection(
                            m.title,
                            m.lessons.map {
                                DetailItem(
                                    it.title,
                                    lessonStatus(it.status, it.score),
                                    it.taskUrl ?: it.recordUrl ?: "",
                                    isLessonDebt(it.type, it.title, it.status, it.passed, it.completedAt, it.attended, it.watchedRecordCredited, it.deadlineAt, now),
                                )
                            },
                        )
                    }
                }
                DetailKind.MATH -> {
                    val dto: MathDetailDto = client.getDecoded(ApiEndpoints.MATH_DETAIL, AppLog.SUBJECTS, "detail-math")
                    val webs = DetailSection(
                        "Вебинары",
                        dto.webinars.map {
                            DetailItem(it.title, "", it.taskUrl ?: it.recordUrl ?: "")
                        },
                    )
                    val days = dto.eventDays.filter { it.events.isNotEmpty() }.map { d ->
                        DetailSection(
                            d.date,
                            d.events.map { DetailItem(it.title, it.status ?: "", "") },
                        )
                    }
                    listOf(webs) + days
                }
            }
        } catch (e: Exception) {
            AppLog.e(AppLog.SUBJECTS, "detailSections failed", e)
            throw e
        }
        val debts = sections.flatMap { it.items }.filter { it.isDebt }
        val rest = sections.map { it.copy(items = it.items.filterNot { s -> s.isDebt }) }
            .filter { it.items.isNotEmpty() }
        val ordered = if (debts.isEmpty()) rest else listOf(DetailSection("Долги · ${debts.size}", debts)) + rest
        AppLog.i(AppLog.SUBJECTS, "detailSections ok n=${ordered.size} debts=${debts.size}")
        emit(ordered)
    }
}
