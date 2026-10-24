package ru.skypro.avatar.student

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.core.network.AccountUnauthorized
import ru.skypro.avatar.student.core.session.SessionLease
import ru.skypro.avatar.student.core.session.StoredCookie
import ru.skypro.avatar.student.data.aihelper.AiChatMessage
import ru.skypro.avatar.student.core.logging.AppLog

class SessionSecurityTest {
    private val origin = Url("https://id.skyeng.ru/login/password")
    private val now = 100_000L

    @Test fun domainBoundaryAndHostOnlyAreEnforced() {
        val hostOnly = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value"), now))
        assertTrue(hostOnly.matches(origin, now))
        assertFalse(hostOnly.matches(Url("https://sub.id.skyeng.ru/login/password"), now))
        val domain = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value", domain = ".skyeng.ru", path = "/"), now))
        assertTrue(domain.matches(Url("https://edu-avatar.skyeng.ru/"), now))
        assertFalse(domain.matches(Url("https://evilskyeng.ru/"), now))
        assertFalse(domain.matches(Url("https://skyeng.ru.evil.org/"), now))
    }

    @Test fun rejectsCookieInjectionFromForeignOriginAndParentDomain() {
        assertNull(StoredCookie.from(Url("https://image.example.org/"), Cookie("sid", "x", domain = ".skyeng.ru"), now))
        assertNull(StoredCookie.from(origin, Cookie("sid", "x", domain = ".ru"), now))
        assertNull(StoredCookie.from(origin, Cookie("sid", "x", domain = "edu-avatar.skyeng.ru"), now))
        assertNull(StoredCookie.from(origin, Cookie("sid", "x", domain = "skyeng.ru."), now))
    }

    @Test fun pathBoundaryAndDefaultPathAreEnforced() {
        val stored = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value"), now))
        assertEquals("/login", stored.path)
        assertTrue(stored.matches(Url("https://id.skyeng.ru/login"), now))
        assertTrue(stored.matches(Url("https://id.skyeng.ru/login/other"), now))
        assertFalse(stored.matches(Url("https://id.skyeng.ru/loginfoo"), now))
        assertFalse(stored.matches(Url("https://id.skyeng.ru/"), now))
    }

    @Test fun secureCookieOnlyTravelsOverHttpsOrWss() {
        val stored = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value", secure = true, path = "/"), now))
        assertTrue(stored.matches(Url("wss://id.skyeng.ru/ws"), now))
        assertFalse(stored.matches(Url("http://id.skyeng.ru/"), now))
        assertFalse(stored.matches(Url("ws://id.skyeng.ru/ws"), now))
        assertNull(StoredCookie.from(Url("http://id.skyeng.ru/"), Cookie("sid", "value", secure = true), now))
    }

    @Test fun maxAgeTakesPrecedenceAndDoesNotRestartAfterPersistence() {
        val stored = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value", maxAge = 10, expires = GMTDate(now - 1)), now))
        val restored = Json.decodeFromString<StoredCookie>(Json.encodeToString(stored))
        assertTrue(restored.matches(origin, now + 9_999))
        assertFalse(restored.matches(origin, now + 10_000))
        assertEquals(stored, restored)
    }

    @Test fun serverDeletionAndExpiryInvalidateCookie() {
        val removed = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value", maxAge = 0, expires = GMTDate(now + 999_999)), now))
        assertFalse(removed.matches(origin, now))
        val expired = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value", expires = GMTDate(now - 1)), now))
        assertFalse(expired.matches(origin, now))
        val original = requireNotNull(StoredCookie.from(origin, Cookie("sid", "value"), now))
        assertTrue(removed.sameIdentity(original))
    }

    @Test fun cookiePrefixesAndRawHeaderSafetyAreValidated() {
        assertNull(StoredCookie.from(origin, Cookie("__Host-sid", "value", secure = true, domain = ".skyeng.ru", path = "/"), now))
        assertNull(StoredCookie.from(origin, Cookie("__Secure-sid", "value"), now))
        assertNotNull(StoredCookie.from(origin, Cookie("__Host-sid", "value", secure = true, path = "/"), now))
        assertNull(StoredCookie.from(origin, Cookie("sid", "value\r\nInjected: header"), now))
        assertNull(StoredCookie.from(origin, Cookie("sid", "value;other=1"), now))
    }

    @Test fun revokedLeaseRejectsCookieWritesAndRepeatedExpiry() {
        var expires = 0
        var writes = 0
        val lease = SessionLease { expires++ }
        lease.unauthorized()
        assertEquals(0, expires)
        lease.authenticate()
        lease.unauthorized()
        lease.unauthorized()
        lease.whileActive { writes++ }
        assertEquals(1, expires)
        assertEquals(0, writes)
    }

    @Test fun lateHttp401CannotExpireNewAccount() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var oldEvents = 0
        var newEvents = 0
        val old = SessionLease { oldEvents++ }.apply { authenticate() }
        val fresh = SessionLease { newEvents++ }.apply { authenticate() }
        val http = HttpClient(MockEngine {
            started.complete(Unit)
            release.await()
            respond("", HttpStatusCode.Unauthorized)
        }) { install(AccountUnauthorized) { lease = old } }
        try {
            val request = async { http.get("https://edu-avatar.skyeng.ru/api/test") }
            started.await()
            old.revoke()
            release.complete(Unit)
            request.await()
            assertEquals(0, oldEvents)
            assertEquals(0, newEvents)
            fresh.unauthorized()
            assertEquals(1, newEvents)
        } finally { http.close() }
    }

    @Test fun activeHttp401IsHandledBeforeDecodingAndForeign401IsIgnored() = runBlocking {
        var events = 0
        val lease = SessionLease { events++ }.apply { authenticate() }
        val http = HttpClient(MockEngine { respond("not json", HttpStatusCode.Unauthorized) }) {
            install(AccountUnauthorized) { this.lease = lease }
        }
        try {
            http.get("https://images.example.org/a")
            assertEquals(0, events)
            http.get("https://edu-avatar.skyeng.ru/a")
            http.get("https://edu-avatar.skyeng.ru/b")
            assertEquals(1, events)
        } finally { http.close() }
    }

    @Test fun repeatedMessagesAndErrorsHaveUniqueStableIds() {
        val messages = List(20) { AiChatMessage(null, "Повтори", true, null) } +
            List(20) { AiChatMessage(null, "Ошибка отправки", false, null) }
        assertEquals(messages.size, messages.map { it.localId }.toSet().size)
        assertEquals(messages[0].localId, messages[0].copy(like = true).localId)
    }

    @Test fun exceptionContentsAreNeverLogged() {
        val before = AppLog.printer
        val logs = mutableListOf<String>()
        try {
            AppLog.printer = { _, _, text -> logs += text }
            AppLog.e(AppLog.TRAINER, "parse failed", IllegalArgumentException("SECRET_BODY"))
            AppLog.w(AppLog.TRAINER, "request failed", IllegalStateException("SECRET_TOKEN"))
            assertFalse(logs.joinToString().contains("SECRET"))
        } finally { AppLog.printer = before }
    }
}
