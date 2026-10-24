package ru.skypro.avatar.student.ui.docs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.skypro.avatar.student.data.docs.KtorDocumentsRepository
import ru.skypro.avatar.student.domain.docs.StudentDocument
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode
import ru.skypro.avatar.student.core.logging.AppLog

class DocumentsViewModel(private val repo: KtorDocumentsRepository) : ViewModel() {
    private val _docs = MutableStateFlow<UiState<List<StudentDocument>>>(UiState.Loading)
    val docs: StateFlow<UiState<List<StudentDocument>>> = _docs
    private var loading: Job? = null

    fun load() {
        AppLog.d(AppLog.DOCS, "vm load")
        _docs.value = UiState.Loading
        loading?.cancel()
        loading = viewModelScope.launch {
            repo.documents()
                .map { UiState.Content(it) as UiState<List<StudentDocument>> }
                .catch { e ->
                    currentCoroutineContext().ensureActive()
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLog.e(AppLog.DOCS, "vm error", e)
                    _docs.value = UiState.Error("Не удалось загрузить документы${httpCode(e)}")
                }
                .collect { currentCoroutineContext().ensureActive(); _docs.value = it }
        }
    }

    suspend fun openUrl(id: Long): String? {
        AppLog.d(AppLog.DOCS, "vm openUrl id=$id")
        return try {
            repo.signedUrl(id).takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.DOCS, "vm openUrl failed id=$id", e)
            null
        }
    }
}
