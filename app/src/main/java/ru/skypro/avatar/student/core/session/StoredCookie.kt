package ru.skypro.avatar.student.core.session

import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.serialization.Serializable

@Serializable
data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val hostOnly: Boolean,
    val secure: Boolean,
    val httpOnly: Boolean,
    val expiresAt: Long?,
    val extensions: Map<String, String?> = emptyMap(),
) {
    fun matches(url: Url, now: Long): Boolean =
        (expiresAt == null || expiresAt > now) &&
            (if (hostOnly) url.host.equals(domain, ignoreCase = true) else domainMatches(url.host, domain)) &&
            (!secure || url.protocol.name in setOf("https", "wss")) &&
            pathMatches(url.encodedPath.ifEmpty { "/" }, path)

    fun toCookie() = Cookie(
        name, value, CookieEncoding.RAW,
        expires = expiresAt?.let { GMTDate(it) },
        domain = domain, path = path, secure = secure, httpOnly = httpOnly, extensions = extensions,
    )

    fun sameIdentity(other: StoredCookie): Boolean =
        name == other.name && domain == other.domain && path == other.path

    companion object {
        fun from(url: Url, cookie: Cookie, now: Long): StoredCookie? {
            val host = url.host.lowercase()
            if (!domainMatches(host, "skyeng.ru")) return null
            val domain = cookie.domain?.removePrefix(".")?.lowercase() ?: host
            if (!domainMatches(domain, "skyeng.ru") || !domainMatches(host, domain)) return null
            if (cookie.secure && url.protocol.name !in setOf("https", "wss")) return null
            if (cookie.name.isEmpty() || cookie.name.any { it <= ' ' || it in ";,=\"\\" }) return null
            if (cookie.value.any { it <= ' ' || it in ";,\"" }) return null
            val path = cookie.path?.takeIf { it.startsWith('/') } ?: defaultPath(url.encodedPath)
            if (cookie.name.startsWith("__Secure-") && !cookie.secure) return null
            if (cookie.name.startsWith("__Host-") &&
                (!cookie.secure || cookie.domain != null || cookie.path != "/")) return null
            val expiry = cookie.maxAge?.let { seconds ->
                if (seconds <= 0) now else now + seconds.toLong() * 1000
            } ?: cookie.expires?.timestamp
            return StoredCookie(
                cookie.name, cookie.value, domain, path, cookie.domain == null,
                cookie.secure, cookie.httpOnly, expiry, cookie.extensions,
            )
        }

        private fun defaultPath(path: String): String =
            if (!path.startsWith('/') || path.lastIndexOf('/') <= 0) "/" else path.substringBeforeLast('/')
    }
}

internal fun domainMatches(host: String, domain: String): Boolean =
    host.equals(domain, ignoreCase = true) || host.endsWith(".$domain", ignoreCase = true)

internal fun pathMatches(request: String, cookie: String): Boolean =
    request == cookie || (request.startsWith(cookie) && (cookie.endsWith('/') || request.getOrNull(cookie.length) == '/'))
