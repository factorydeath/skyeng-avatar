package ru.skypro.avatar.student

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.skypro.avatar.student.core.network.dto.ProfessionDetailDto
import ru.skypro.avatar.student.core.network.dto.SubjectsResponseDto
import ru.skypro.avatar.student.core.network.dto.WeekScheduleDto
import ru.skypro.avatar.student.data.schedule.toDomain
import ru.skypro.avatar.student.data.subjects.toDomain

class ParsingTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    @Test
    fun weeklyParses() {
        val raw = """{"timezone":"Europe/Moscow","lunch":{"start":"12:45","end":"13:15"},"days":[{"date":"2026-10-09","slots":[{"slotId":1735,"activityType":"lesson","subject":"math","title":"Trainer","startsAt":"2026-10-09T09:30:00+03:00","endsAt":"2026-10-09T10:15:00+03:00"}]}]}"""
        val dto = json.decodeFromString<WeekScheduleDto>(raw)
        val week = dto.toDomain()
        assertEquals("2026-10-09", week.days.first().date)
        assertEquals(1735L, week.days.first().slots.first().id)
        assertEquals("math", week.days.first().slots.first().subjectCode)
    }

    @Test
    fun subjectsParse() {
        val raw = """{"subjects":[{"subjectCode":"math","lessonProgress":{"completed":4,"total":10},"debts":{"count":3,"rating":"normal"},"isBlocked":false}]}"""
        val dto = json.decodeFromString<SubjectsResponseDto>(raw)
        val s = dto.subjects.map { it.toDomain() }.first()
        assertEquals("math", s.code)
        assertEquals(3, s.debts)
    }

    @Test
    fun professionParses() {
        val raw = """{"programId":1,"programTitle":"Soft","modules":[{"moduleTitle":"M1","moduleId":1,"firstEventDate":"2026-09-07T21:00:30+00:00","lessons":[{"id":264468,"streamLessonTitle":"Practice","streamLessonLink":"https://example/1","startAt":"2026-09-07T21:00:30+00:00","completeness":100,"lessonType":"Regular","homework":{"has":true,"isHomeworkCompleted":true,"isDebt":false}}],"lives":[],"isComplete":false}]}"""
        val dto = json.decodeFromString<ProfessionDetailDto>(raw)
        val lesson = dto.modules.first().lessons.first()
        assertEquals(264468L, lesson.id)
        assertEquals("https://example/1", lesson.streamLessonLink)
    }
}
