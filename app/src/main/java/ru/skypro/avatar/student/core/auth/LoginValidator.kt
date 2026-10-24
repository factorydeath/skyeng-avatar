package ru.skypro.avatar.student.core.auth

object LoginValidator {
    fun emailError(email: String): String? =
        if (email.contains("@") && email.contains(".")) null else "Введите корректную почту"

    fun passwordError(password: String): String? =
        if (password.length >= 6) null else "Минимум 6 символов"
}
