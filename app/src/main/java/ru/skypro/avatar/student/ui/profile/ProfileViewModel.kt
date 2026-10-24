package ru.skypro.avatar.student.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.dto.AcademicSummaryDto
import ru.skypro.avatar.student.core.network.dto.HelpfulLinksDto
import ru.skypro.avatar.student.core.network.dto.QuestionnaireDto
import ru.skypro.avatar.student.data.day.KtorDayExtrasRepository
import ru.skypro.avatar.student.data.profile.KtorProfileRepository
import ru.skypro.avatar.student.domain.day.BrsSummary
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.state.httpCode

data class ProfileContent(
    val summary: AcademicSummaryDto = AcademicSummaryDto(),
    val interests: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val goal: String? = null,
    val format: String? = null,
    val personalRouteUrl: String? = null,
    val personalRouteTitle: String? = null,
)

class ProfileViewModel(
    private val repo: KtorProfileRepository,
    private val dayExtras: KtorDayExtrasRepository,
) : ViewModel() {
    private val _summary = MutableStateFlow<UiState<ProfileContent>>(UiState.Loading)
    val summary: StateFlow<UiState<ProfileContent>> = _summary
    private val _brs = MutableStateFlow<UiState<BrsSummary?>>(UiState.Loading)
    val brs: StateFlow<UiState<BrsSummary?>> = _brs
    private var summaryJob: Job? = null
    private var brsJob: Job? = null

    fun load() {
        AppLog.d(AppLog.PROFILE, "vm load")
        _summary.value = UiState.Loading
        retryBrs()
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            try {
                combine(
                    repo.summary(),
                    repo.questionnaire(),
                    repo.helpfulLinks(),
                ) { s: AcademicSummaryDto, q: QuestionnaireDto, l: HelpfulLinksDto ->
                    ProfileContent(
                        summary = s,
                        interests = q.data?.selectedInterests ?: emptyList(),
                        favorites = q.data?.favoriteInterests ?: emptyList(),
                        goal = q.data?.goal,
                        format = q.data?.format,
                        personalRouteUrl = l.personalRoute?.url,
                        personalRouteTitle = l.personalRoute?.title,
                    )
                }.catch { e ->
                    currentCoroutineContext().ensureActive()
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLog.e(AppLog.PROFILE, "vm error", e)
                    _summary.value = UiState.Error("Не удалось загрузить профиль${httpCode(e)}")
                }.collect { currentCoroutineContext().ensureActive(); _summary.value = UiState.Content(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                AppLog.e(AppLog.PROFILE, "vm error", e)
                _summary.value = UiState.Error("Не удалось загрузить профиль${httpCode(e)}")
            }
        }
    }

    fun retryBrs() {
        _brs.value = UiState.Loading
        brsJob?.cancel()
        brsJob = viewModelScope.launch { loadBrs() }
    }

    private suspend fun loadBrs() {
        try {
            dayExtras.rating().collect { currentCoroutineContext().ensureActive(); _brs.value = UiState.Content(it) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            AppLog.e(AppLog.PROFILE, "vm brs error", e)
            _brs.value = UiState.Error("Не удалось загрузить баллы${httpCode(e)}")
        }
    }
}
