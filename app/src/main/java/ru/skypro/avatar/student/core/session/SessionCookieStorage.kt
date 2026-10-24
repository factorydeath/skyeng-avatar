package ru.skypro.avatar.student.core.session

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionCookieStorage(
    private val persistence: SessionPersistence,
    private val lease: SessionLease,
) : CookiesStorage {
    override suspend fun get(requestUrl: Url): List<Cookie> = withContext(Dispatchers.IO) {
        lease.whileActive {
            val now = System.currentTimeMillis()
            val stored = persistence.readCookies()
            val alive = stored.filter { it.expiresAt == null || it.expiresAt > now }
            if (alive.size != stored.size) persistence.writeCookies(alive)
            alive.filter { it.matches(requestUrl, now) }.sortedByDescending { it.path.length }.map { it.toCookie() }
        } ?: emptyList()
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = withContext(Dispatchers.IO) {
        lease.whileActive {
            val now = System.currentTimeMillis()
            val incoming = StoredCookie.from(requestUrl, cookie, now) ?: return@whileActive
            val cookies = persistence.readCookies().filterNot {
                it.sameIdentity(incoming) || (it.expiresAt != null && it.expiresAt <= now)
            }.toMutableList()
            if (incoming.expiresAt == null || incoming.expiresAt > now) cookies += incoming
            persistence.writeCookies(cookies)
        }
        Unit
    }

    override fun close() = Unit
}
