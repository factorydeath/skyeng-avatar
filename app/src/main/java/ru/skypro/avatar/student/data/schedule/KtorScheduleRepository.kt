package ru.skypro.avatar.student.data.schedule

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.decodeOrThrow
import ru.skypro.avatar.student.core.network.retryApi
import ru.skypro.avatar.student.core.network.dto.DayScheduleDto
import ru.skypro.avatar.student.core.network.dto.WeekScheduleDto
import ru.skypro.avatar.student.domain.schedule.DaySchedule
import ru.skypro.avatar.student.domain.schedule.WeekSchedule
import ru.skypro.avatar.student.core.time.studyClock

class KtorScheduleRepository(private val client: HttpClient) : ScheduleRepository {
    override fun week(startIsoDate: String, endIsoDate: String): Flow<WeekSchedule> = flow {
        AppLog.d(AppLog.SCHEDULE, "week $startIsoDate..$endIsoDate")
        try {
            val dto: WeekScheduleDto = retryApi(AppLog.SCHEDULE, "week") {
                client.get(ApiEndpoints.TIMETABLE_WEEKLY) {
                    parameter("start", startIsoDate)
                    parameter("end", endIsoDate)
                }.decodeOrThrow(AppLog.SCHEDULE, "week")
            }
            AppLog.i(AppLog.SCHEDULE, "week ok days=${dto.days.size}")
            client.studyClock().accept(dto.timezone)
            emit(dto.toDomain())
        } catch (e: Exception) {
            AppLog.e(AppLog.SCHEDULE, "week failed", e)
            throw e
        }
    }

    override fun day(dateIso: String): Flow<DaySchedule> = flow {
        AppLog.d(AppLog.SCHEDULE, "day $dateIso")
        try {
            val dto: DayScheduleDto = retryApi(AppLog.SCHEDULE, "day") {
                client.get(ApiEndpoints.TIMETABLE_DAY) {
                    parameter("date", dateIso)
                }.decodeOrThrow(AppLog.SCHEDULE, "day")
            }
            AppLog.i(AppLog.SCHEDULE, "day ok slots=${dto.slots.size}")
            client.studyClock().accept(dto.timezone)
            emit(dto.toDomain())
        } catch (e: Exception) {
            AppLog.e(AppLog.SCHEDULE, "day failed", e)
            throw e
        }
    }
}
