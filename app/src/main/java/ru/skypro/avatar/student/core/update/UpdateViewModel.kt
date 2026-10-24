package ru.skypro.avatar.student.core.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface UpdateBackend {
    suspend fun check(): UpdateCheck
    suspend fun download(version: String, onProgress: (Int?) -> Unit): File
    fun canInstall(): Boolean
    fun openSettings(): Boolean
    fun install(apk: File, version: String): Boolean
}

data class UpdateState(
    val version: String? = null,
    val downloading: Boolean = false,
    val progress: Int? = null,
    val apk: File? = null,
    val awaitingPermission: Boolean = false,
    val error: String? = null,
)

class UpdateViewModel(private val backend: UpdateBackend) : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state
    private var downloadJob: Job? = null
    @Volatile private var generation = 0

    init {
        viewModelScope.launch {
            try {
                val result = backend.check()
                if (result is UpdateCheck.Available) _state.value = UpdateState(version = result.remote)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun downloadOrInstall() {
        val current = _state.value
        val version = current.version ?: return
        if (current.downloading || downloadJob?.isActive == true || current.awaitingPermission) return
        if (current.apk != null) { requestInstallation(); return }
        val token = ++generation
        _state.value = current.copy(downloading = true, progress = null, error = null)
        downloadJob = viewModelScope.launch {
            try {
                val apk = backend.download(version) { progress ->
                    _state.update { if (generation == token && it.downloading) it.copy(progress = progress) else it }
                }
                if (generation != token) { apk.delete(); return@launch }
                _state.value = _state.value.copy(downloading = false, progress = null, apk = apk)
                requestInstallation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (generation == token) _state.value = _state.value.copy(
                    downloading = false, progress = null,
                    error = "Не удалось скачать или проверить обновление. Проверьте интернет и попробуйте снова.",
                )
            }
        }
    }

    private fun requestInstallation() {
        if (backend.canInstall()) installReady()
        else {
            _state.value = _state.value.copy(awaitingPermission = true, error = null)
            if (!backend.openSettings()) _state.value = _state.value.copy(
                awaitingPermission = false, error = "Не удалось открыть настройки. Разрешите установку из этого приложения вручную.",
            )
        }
    }

    fun onSettingsReturned() {
        if (!_state.value.awaitingPermission) return
        _state.value = _state.value.copy(awaitingPermission = false)
        if (backend.canInstall()) installReady()
        else _state.value = _state.value.copy(error = "Для обновления разрешите установку из этого приложения.")
    }

    private fun installReady() {
        val current = _state.value
        val apk = current.apk ?: return
        val version = current.version ?: return
        if (backend.install(apk, version)) {
            _state.value = UpdateState()
        } else {
            apk.delete()
            _state.value = current.copy(apk = null, error = "Не удалось проверить APK или открыть установщик. Попробуйте скачать обновление ещё раз.")
        }
    }

    fun dismiss() {
        generation++
        downloadJob?.cancel()
        downloadJob = null
        _state.value.apk?.delete()
        _state.value = UpdateState()
    }
}
