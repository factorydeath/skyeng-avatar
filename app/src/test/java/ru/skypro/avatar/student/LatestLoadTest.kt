package ru.skypro.avatar.student

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.core.time.StudyClock
import ru.skypro.avatar.student.data.schedule.ScheduleRepository
import ru.skypro.avatar.student.domain.schedule.*
import ru.skypro.avatar.student.ui.schedule.ScheduleViewModel
import ru.skypro.avatar.student.ui.myday.MyDayViewModel
import ru.skypro.avatar.student.ui.state.UiState

@OptIn(ExperimentalCoroutinesApi::class)
class LatestLoadTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun olderWeekCannotOverwriteNewWeek() = runTest(dispatcher) {
        var canceled = false
        val repo = object : ScheduleRepository {
            override fun day(dateIso: String): Flow<DaySchedule> = error("unused")
            override fun week(startIsoDate: String, endIsoDate: String) = flow {
                try {
                    if (startIsoDate == "old") delay(10_000)
                    emit(WeekSchedule("UTC", startIsoDate, emptyList()))
                } finally { if (startIsoDate == "old") canceled = true }
            }
        }
        val vm = ScheduleViewModel(repo)
        vm.loadWeek("old", "old"); runCurrent()
        vm.loadWeek("new", "new"); advanceUntilIdle()
        assertTrue(canceled)
        assertEquals("new", (vm.week.value as UiState.Content).data.lunchLabel)
    }

    @Test fun firstDayResponseCorrectsPhoneDateUsingApiTimezone() = runTest(dispatcher) {
        val dates = mutableListOf<String>()
        val clock = StudyClock(Clock.fixed(Instant.parse("2026-10-11T22:30:00Z"), ZoneOffset.UTC)).apply { accept("UTC") }
        val repo = object : ScheduleRepository {
            override fun week(startIsoDate: String, endIsoDate: String): Flow<WeekSchedule> = error("unused")
            override fun day(dateIso: String) = flow {
                dates += dateIso
                emit(DaySchedule(dateIso, emptyList(), "Europe/Moscow"))
            }
        }
        val vm = MyDayViewModel(repo, clock)
        vm.load(); advanceUntilIdle()
        assertEquals(listOf("2026-10-11", "2026-10-12"), dates)
        assertEquals("2026-10-12", (vm.state.value as UiState.Content).data.date.toString())
    }
}
