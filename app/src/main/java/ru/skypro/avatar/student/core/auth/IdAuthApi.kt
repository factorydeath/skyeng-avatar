package ru.skypro.avatar.student.core.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.retryApi
import ru.skypro.avatar.student.core.network.ApiException

class IdAuthApi(private val client: HttpClient) {
    suspend fun login(username: String, password: String): Boolean {
        AppLog.d(AppLog.AUTH, "login start userLen=${username.length}")
        val html: String = retryApi(AppLog.AUTH, "login page") {
            val response = client.get(ApiEndpoints.LOGIN_PAGE)
            if (response.status.value !in 200..299) throw ApiException.Http(response.status.value)
            response.bodyAsText()
        }
        val csrf = Regex("""name="csrfToken" value="([^"]+)"""").find(html)?.groupValues?.get(1)
        if (csrf == null) {
            AppLog.w(AppLog.AUTH, "login csrf not found htmlLen=${html.length}")
            return false
        }
        AppLog.d(AppLog.AUTH, "login csrf ok")
        val submit = client.submitForm(
            url = ApiEndpoints.LOGIN_SUBMIT,
            formParameters = parameters {
                append("csrfToken", csrf)
                append("redirect", "https://avatar.skyeng.ru/")
                append("username", username)
                append("password", password)
            },
        )
        AppLog.i(AppLog.AUTH, "login submit status=${submit.status}")
        if (submit.status != HttpStatusCode.OK) return false
        return checkSession()
    }

    suspend fun checkSession(): Boolean {
        val jwt = client.post(ApiEndpoints.AUTH_JWT)
        AppLog.d(AppLog.AUTH, "jwt status=${jwt.status}")
        return jwt.status == HttpStatusCode.OK
    }
}
