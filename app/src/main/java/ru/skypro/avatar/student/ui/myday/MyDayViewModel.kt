package ru.skypro.avatar.student.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.skypro.avatar.student.data.schedule.ScheduleRepository
import ru.skypro.avatar.student.domain.schedule.Slot
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode
import ru.skypro.avatar.student.core.logging.AppLog
import java.time.LocalDate
import ru.skypro.avatar.student.core.time.StudyClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

data class MyDayContent(
    val slots: List<Slot> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val timeZone: String = "",
)

class MyDayViewModel(
    private val schedule: ScheduleRepository,
    private val clock: StudyClock = StudyClock(),
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<MyDayContent>>(UiState.Loading)
    val state: StateFlow<UiState<MyDayContent>> = _state
    private var loading: Job? = null

    fun load() {
        AppLog.d(AppLog.DAY, "vm load")
        _state.value = UiState.Loading
        loading?.cancel()
        loading = viewModelScope.launch {
            try {
                val requested = clock.today()
                var day = schedule.day(requested.toString()).first()
                clock.accept(day.timeZone)
                val today = clock.today()
                if (today != requested) day = schedule.day(today.toString()).first()
                currentCoroutineContext().ensureActive()
                _state.value = UiState.Content(MyDayContent(day.slots, today, day.timeZone))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _state.value = UiState.Error("Не удалось загрузить день${httpCode(e)}")
            }
        }
    }
}
