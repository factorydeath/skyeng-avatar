package ru.skypro.avatar.student.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.skypro.avatar.student.data.schedule.ScheduleRepository
import ru.skypro.avatar.student.domain.schedule.WeekSchedule
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.time.StudyClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class ScheduleViewModel(private val repo: ScheduleRepository, private val clock: StudyClock = StudyClock()) : ViewModel() {
    private val _week = MutableStateFlow<UiState<WeekSchedule>>(UiState.Loading)
    val week: StateFlow<UiState<WeekSchedule>> = _week
    private var loading: Job? = null

    fun loadCurrentWeek() {
        val monday = clock.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        loadWeek(monday.toString(), monday.plusDays(6).toString(), currentWeek = true)
    }

    fun loadWeek(startIso: String, endIso: String, currentWeek: Boolean = false) {
        AppLog.d(AppLog.SCHEDULE, "vm loadWeek")
        _week.value = UiState.Loading
        loading?.cancel()
        loading = viewModelScope.launch {
            try {
                var data = repo.week(startIso, endIso).first()
                clock.accept(data.timeZone)
                val monday = clock.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                if (currentWeek && monday.toString() != startIso) data = repo.week(monday.toString(), monday.plusDays(6).toString()).first()
                currentCoroutineContext().ensureActive()
                _week.value = UiState.Content(data)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _week.value = UiState.Error("Не удалось загрузить расписание${httpCode(e)}")
            }
        }
    }
}
