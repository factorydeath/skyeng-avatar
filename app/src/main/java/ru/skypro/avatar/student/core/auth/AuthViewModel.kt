package ru.skypro.avatar.student.core.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.session.AccountSession
import ru.skypro.avatar.student.core.session.SessionPersistence

class AuthViewModel(context: Context) : ViewModel() {
    private val persistence = SessionPersistence(context.applicationContext)
    private val _state = MutableStateFlow<AuthState>(AuthState.Restoring)
    val state: StateFlow<AuthState> = _state
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice
    private val _account = MutableStateFlow<AccountSession?>(null)
    val account: StateFlow<AccountSession?> = _account
    private var current: AccountSession? = null
    private var operation: Job? = null
    private var restored = false

    private fun retire() {
        current?.close()
        current = null
        _account.value = null
    }

    private fun newSession(): AccountSession = AccountSession(persistence) { expired ->
        viewModelScope.launch {
            if (current === expired) endSession("Сессия истекла. Войдите заново.")
        }
    }.also { current = it }

    fun restore() {
        if (restored) return
        restored = true
        operation = viewModelScope.launch {
            try {
                val loggedIn = withContext(Dispatchers.IO) {
                    persistence.initialize()
                    persistence.loggedIn()
                }
                if (persistence.recovered) _notice.value = "Хранилище сессии обновлено или восстановлено. Войдите заново."
                if (!loggedIn) {
                    _state.value = AuthState.LoggedOut
                    return@launch
                }
                val session = newSession()
                if (IdAuthApi(session.client).checkSession()) {
                    session.lease.authenticate()
                    _account.value = session
                    _state.value = AuthState.LoggedIn
                } else {
                    retire()
                    withContext(Dispatchers.IO) { persistence.clear() }
                    _notice.value = "Сессия истекла. Войдите заново."
                    _state.value = AuthState.LoggedOut
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun login(email: String, password: String) {
        if (_state.value == AuthState.Loading || _state.value == AuthState.Restoring) return
        val mail = email.trim()
        val pass = password.trim()
        if (LoginValidator.emailError(mail) != null || LoginValidator.passwordError(pass) != null) return
        operation?.cancel()
        retire()
        _state.value = AuthState.Loading
        operation = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    persistence.initialize()
                    persistence.clear()
                }
                val session = newSession()
                if (IdAuthApi(session.client).login(mail, pass)) {
                    withContext(Dispatchers.IO) { persistence.markLoggedIn() }
                    session.lease.authenticate()
                    _notice.value = null
                    _account.value = session
                    _state.value = AuthState.LoggedIn
                } else {
                    retire()
                    withContext(Dispatchers.IO) { persistence.clear() }
                    _notice.value = null
                    _state.value = AuthState.NeedLogin
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun logout() = endSession(null)

    private fun endSession(message: String?) {
        operation?.cancel()
        retire()
        _notice.value = message
        _state.value = AuthState.Loading
        operation = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { persistence.clear() }
                _state.value = AuthState.LoggedOut
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun fail(e: Exception) {
        retire()
        AppLog.e(AppLog.AUTH, "session operation failed", e)
        _notice.value = "Не удалось открыть или сохранить сессию. Повторите вход."
        _state.value = AuthState.NetworkError
    }

    override fun onCleared() {
        retire()
    }
}
