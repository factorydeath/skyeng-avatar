package ru.skypro.avatar.student

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.core.network.ApiJson
import ru.skypro.avatar.student.core.network.dto.*
import ru.skypro.avatar.student.core.network.externalWebUrl
import ru.skypro.avatar.student.core.time.*
import ru.skypro.avatar.student.data.day.toBrsSummary
import ru.skypro.avatar.student.data.schedule.toDomain
import ru.skypro.avatar.student.domain.day.*

class DataCorrectnessTest {
    @Test fun scheduleConvertsOffsetsAndKeepsLocalWallTime() {
        assertEquals("03:30", scheduleTime("2026-10-12T00:30:00Z", "Europe/Moscow"))
        assertEquals("20:30", scheduleTime("2026-10-12T00:30:00Z", "America/New_York"))
        assertEquals("10:30", scheduleTime("2026-10-12T10:30:00", "Europe/Moscow"))
        assertEquals("10:30", scheduleTime("2026-10-12T10:30:00+03:00", "invalid"))
        assertEquals("—", scheduleTime("bad", "Europe/Moscow"))
    }

    @Test fun clockUsesApiDayAndRejectsInvalidZone() {
        val clock = StudyClock(Clock.fixed(Instant.parse("2026-10-11T22:30:00Z"), ZoneOffset.UTC))
        clock.accept("Europe/Moscow")
        assertEquals("2026-10-12", clock.today().toString())
        clock.accept("invalid")
        assertEquals("Europe/Moscow", clock.zone.id)
    }

    @Test fun dayAndWeekKeepTimezoneOnSlots() {
        val day = DayScheduleDto(timezone = "Europe/Moscow", slots = listOf(SlotDto())).toDomain()
        assertEquals("Europe/Moscow", day.timeZone)
        assertEquals(day.timeZone, day.slots.single().timeZone)
        val week = WeekScheduleDto(timezone = "Europe/Moscow", days = listOf(DayDto(slots = listOf(SlotDto())))).toDomain()
        assertEquals(week.timeZone, week.days.single().slots.single().timeZone)
    }

    @Test fun absentAndUnavailableBrsAreNotZeroScores() {
        assertNull(ApiJson.decodeFromString<BrsResponseDto>("""{"availability":"unavailable","rating":null}""").toBrsSummary())
        assertNull(BrsResponseDto().toBrsSummary())
        val rating = BrsRatingDto(currentTotal = BrsTotalDto(), disciplinary_points = BrsDisciplinaryDto())
        assertNull(BrsResponseDto(rating, "unavailable").toBrsSummary())
        assertNotNull(BrsResponseDto(rating, "available").toBrsSummary())
        assertTrue(runCatching { BrsResponseDto(BrsRatingDto()).toBrsSummary() }.isFailure)
    }

    @Test fun headerBarMatchesEarnedOverAvailable() {
        val dto = BrsResponseDto(
            BrsRatingDto(
                currentTotal = BrsTotalDto(earned = 276.17, available = 377.99, planned = 999.4, percent = 73),
                disciplinary_points = BrsDisciplinaryDto(current = 195, initial = 200),
            ),
            "available",
        )
        val summary = dto.toBrsSummary()!!
        assertEquals(377.99, summary.totalAvailable, 0.0)
        assertEquals(73.0, summary.totalEarned / summary.totalAvailable * 100, 0.6)
        assertEquals("276,2", formatScore(summary.totalEarned))
        assertEquals("378,0", formatScore(summary.totalAvailable))
    }

    @Test fun chartTracksAvailableAndEarnedOnSameScale() {
        val weeks = listOf(BrsWeek(1, 20.0, 40.0), BrsWeek(2, 80.0, 100.0))
        val max = brsChartMaximum(weeks)
        assertEquals(100.0, max, 0.0)
        assertEquals(0.4f, brsChartFraction(weeks[0].available, max))
        assertEquals(0.2f, brsChartFraction(weeks[0].earned, max))
        assertEquals(0f, brsChartFraction(-1.0, max))
        assertEquals(0f, brsChartFraction(Double.NaN, max))
        assertEquals(120.0, brsChartMaximum(listOf(BrsWeek(1, 120.0, 100.0))), 0.0)
    }

    @Test fun linksResolveRelativeUrlsAndRejectUnsupportedSchemes() {
        assertEquals("https://avatar.skyeng.ru/documents?a=1", externalWebUrl("/documents?a=1"))
        assertEquals("https://example.org/file?a=1%202", externalWebUrl("https://example.org/file?a=1%202"))
        for (url in listOf("", "javascript:alert(1)", "file:///etc/file", "intent://x", "https://user:pass@example.org/", "https://", "https://exa mple.org", "https://example.org\\x")) {
            assertNull(url, externalWebUrl(url))
        }
    }
}
