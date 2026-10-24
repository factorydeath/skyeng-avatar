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
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.data.subjects.SubjectsRepository
import ru.skypro.avatar.student.domain.subjects.DetailSection
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode

class SubjectDetailViewModel(private val repo: SubjectsRepository) : ViewModel() {
    private val _detail = MutableStateFlow<UiState<List<DetailSection>>>(UiState.Loading)
    val detail: StateFlow<UiState<List<DetailSection>>> = _detail
    private var loading: Job? = null

    fun load(subjectEnum: String) {
        AppLog.d(AppLog.SUBJECTS, "vm detail $subjectEnum")
        _detail.value = UiState.Loading
        loading?.cancel()
        loading = viewModelScope.launch {
            repo.detailSections(subjectEnum)
                .map { UiState.Content(it) as UiState<List<DetailSection>> }
                .catch { e ->
                    currentCoroutineContext().ensureActive()
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLog.e(AppLog.SUBJECTS, "vm detail error", e)
                    _detail.value = UiState.Error("Не удалось загрузить предмет${httpCode(e)}")
                }
                .collect { currentCoroutineContext().ensureActive(); _detail.value = it }
        }
    }
}
