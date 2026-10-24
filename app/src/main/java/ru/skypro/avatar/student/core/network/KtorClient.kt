package ru.skypro.avatar.student.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.skypro.avatar.student.BuildConfig
import ru.skypro.avatar.student.core.session.SessionLease
import ru.skypro.avatar.student.core.session.SessionLeaseKey
import io.ktor.client.plugins.api.createClientPlugin

internal val AccountUnauthorized = createClientPlugin("AccountUnauthorized", ::AccountUnauthorizedConfig) {
    val lease = pluginConfig.lease
    onResponse { response ->
        if (response.status.value == 401 && response.request.url.host.let {
                it == "skyeng.ru" || it.endsWith(".skyeng.ru")
            }) lease?.unauthorized()
    }
}

internal class AccountUnauthorizedConfig {
    var lease: SessionLease? = null
}

fun createHttpClient(cookiesStorage: CookiesStorage? = null, lease: SessionLease? = null): HttpClient = HttpClient(OkHttp) {
    install(AccountUnauthorized) { this.lease = lease }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true })
    }
    install(HttpCookies) {
        if (cookiesStorage != null) storage = cookiesStorage
    }
    install(DefaultRequest) {
        accept(ContentType.Application.Json)
        header("Referer", "https://avatar.skyeng.ru/")
        header("Origin", "https://avatar.skyeng.ru")
        header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36")
    }
    install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    if (BuildConfig.DEBUG) install(Logging) { level = LogLevel.INFO }
    install(WebSockets)
}.also { client -> if (lease != null) client.attributes.put(SessionLeaseKey, lease) }
