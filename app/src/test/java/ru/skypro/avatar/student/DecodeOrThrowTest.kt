package ru.skypro.avatar.student

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.skypro.avatar.student.core.network.ApiException
import ru.skypro.avatar.student.core.network.decodeOrThrow

@Serializable
private data class Sample(val status: String = "", val nextTaskId: String? = null)

class DecodeOrThrowTest {
    private val jsonHeaders: Headers = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(status: HttpStatusCode, body: String, headers: Headers = jsonHeaders) =
        HttpClient(MockEngine { _ -> respond(body, status, headers) })

    @Test
    fun decodesSuccessfulJson() = runBlocking {
        val http = client(HttpStatusCode.OK, """{"status":"ok","nextTaskId":"t1"}""")
        val dto: Sample = http.get("/x").decodeOrThrow("test", "ok")
        assertEquals("ok", dto.status)
        assertEquals("t1", dto.nextTaskId)
    }

    @Test
    fun serverErrorThrowsHttpWithCode() = runBlocking {
        val http = client(HttpStatusCode.InternalServerError, """{"error":"boom"}""")
        try {
            http.get("/x").decodeOrThrow<Sample>("test", "err")
            throw AssertionError("expected exception")
        } catch (e: ApiException.Http) {
            assertEquals(500, e.httpCode)
        }
    }

    @Test
    fun unauthorizedThrowsUnauthorized() = runBlocking {
        val http = client(HttpStatusCode.Unauthorized, "")
        try {
            http.get("/x").decodeOrThrow<Sample>("test", "auth")
            throw AssertionError("expected exception")
        } catch (e: ApiException.Unauthorized) {
            assertTrue(e.httpCode == 401)
        }
    }

    @Test
    fun htmlBodyOnSuccessThrowsParse() = runBlocking {
        val http = client(HttpStatusCode.OK, "<html>Bad Gateway</html>", headersOf(HttpHeaders.ContentType, "text/html"))
        try {
            http.get("/x").decodeOrThrow<Sample>("test", "html")
            throw AssertionError("expected exception")
        } catch (e: ApiException.Parse) {
            assertTrue(e.message != null)
        }
    }

    @Test
    fun emptyBodyOnSuccessThrowsParse() = runBlocking {
        val http = client(HttpStatusCode.OK, "")
        try {
            http.get("/x").decodeOrThrow<Sample>("test", "empty")
            throw AssertionError("expected exception")
        } catch (e: ApiException.Parse) {
            assertTrue(e.message != null)
        }
    }
}
