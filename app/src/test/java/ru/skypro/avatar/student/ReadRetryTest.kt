package ru.skypro.avatar.student

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.core.network.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReadRetryTest {
    @Serializable private data class Result(val value: Int)

    @Test fun retriesTemporaryHttpFailureAndStopsAfterSuccess() = runTest {
        var requests = 0
        val http = HttpClient(MockEngine {
            requests++
            if (requests < 3) respond("", HttpStatusCode.ServiceUnavailable) else respond("""{"value":42}""")
        })
        try {
            assertEquals(42, http.getDecoded<Result>("https://example.test/", "test", "get").value)
            assertEquals(3, requests)
        } finally { http.close() }
    }

    @Test fun permanentHttpAndMalformedJsonAreNotRetried() = runTest {
        for ((status, body) in listOf(400 to "", 401 to "", 403 to "", 404 to "", 422 to "", 200 to "not json")) {
            var requests = 0
            val http = HttpClient(MockEngine { requests++; respond(body, HttpStatusCode.fromValue(status)) })
            try {
                assertTrue(runCatching { http.getDecoded<Result>("https://example.test/", "test", "get") }.isFailure)
                assertEquals("HTTP $status", 1, requests)
            } finally { http.close() }
        }
    }

    @Test fun networkFailureHasThreeAttemptsAndCancellationHasOne() = runTest {
        var attempts = 0
        runCatching { retryApi<Unit>("test", "get") { attempts++; throw IOException("offline") } }
        assertEquals(3, attempts)
        attempts = 0
        val result = runCatching { retryApi<Unit>("test", "get") { attempts++; throw CancellationException("closed") } }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(1, attempts)
    }
}
