package ru.skypro.avatar.student.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.skypro.avatar.student.data.subjects.SubjectsRepository
import ru.skypro.avatar.student.domain.subjects.Subject
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode
import ru.skypro.avatar.student.core.logging.AppLog

class SubjectsViewModel(private val repo: SubjectsRepository) : ViewModel() {
    private val _subjects = MutableStateFlow<UiState<List<Subject>>>(UiState.Loading)
    val subjects: StateFlow<UiState<List<Subject>>> = _subjects
    private var loading: Job? = null

    fun load() {
        AppLog.d(AppLog.SUBJECTS, "vm load")
        _subjects.value = UiState.Loading
        loading?.cancel()
        loading = viewModelScope.launch {
            repo.subjects()
                .map { UiState.Content(it) as UiState<List<Subject>> }
                .catch { e ->
                    currentCoroutineContext().ensureActive()
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLog.e(AppLog.SUBJECTS, "vm error", e)
                    _subjects.value = UiState.Error("Не удалось загрузить предметы${httpCode(e)}")
                }
                .collect { currentCoroutineContext().ensureActive(); _subjects.value = it }
        }
    }
}
