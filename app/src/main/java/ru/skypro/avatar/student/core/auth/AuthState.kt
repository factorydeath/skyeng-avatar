package ru.skypro.avatar.student.core.auth

sealed interface AuthState {
    data object LoggedOut : AuthState
    data object Restoring : AuthState
    data object Loading : AuthState
    data object LoggedIn : AuthState
    data object NeedLogin : AuthState
    data object NetworkError : AuthState
}
