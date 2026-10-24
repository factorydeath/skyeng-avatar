package ru.skypro.avatar.student.data.schedule

import kotlinx.coroutines.flow.Flow
import ru.skypro.avatar.student.domain.schedule.DaySchedule
import ru.skypro.avatar.student.domain.schedule.WeekSchedule

interface ScheduleRepository {
    fun week(startIsoDate: String, endIsoDate: String): Flow<WeekSchedule>
    fun day(dateIso: String): Flow<DaySchedule>
}
