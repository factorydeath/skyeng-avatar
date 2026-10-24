package ru.skypro.avatar.student

import java.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.skypro.avatar.student.core.network.ApiException
import ru.skypro.avatar.student.core.network.ApiJson
import ru.skypro.avatar.student.core.network.dto.FinishTaskResponseDto
import ru.skypro.avatar.student.core.network.dto.NextTaskStatusDto
import ru.skypro.avatar.student.core.network.toApiException
import ru.skypro.avatar.student.core.network.userMessage

class ApiErrorsTest {
    @Test
    fun ignoresUnknownFieldsInFinishTaskResponse() {
        val dto = ApiJson.decodeFromString<FinishTaskResponseDto>(
            """{"status":"ok","nextTaskId":"abc","extra":{"x":1}}""",
        )
        assertEquals("ok", dto.status)
        assertEquals("abc", dto.nextTaskId)
    }

    @Test
    fun transitionIdFromServerIsNumber() {
        val dto = ApiJson.decodeFromString<FinishTaskResponseDto>(
            """{"nextTaskId":null,"status":"calculation_in_progress","transitionId":823274}""",
        )
        assertEquals("calculation_in_progress", dto.status)
        assertEquals(823274L, dto.transitionId)
    }

    @Test
    fun missingOptionalFieldsBecomeNull() {
        val dto = ApiJson.decodeFromString<NextTaskStatusDto>("""{"status":"finished"}""")
        assertEquals("finished", dto.status)
        assertNull(dto.taskId)
        assertNull(dto.error)
    }

    @Test
    fun nullFieldsAreAcceptedAsDefaults() {
        val dto = ApiJson.decodeFromString<FinishTaskResponseDto>("""{"status":null,"nextTaskId":null}""")
        assertEquals("", dto.status)
        assertNull(dto.nextTaskId)
    }

    @Test(expected = SerializationException::class)
    fun htmlBodyIsNotJson() {
        ApiJson.decodeFromString<FinishTaskResponseDto>("<html>502 Bad Gateway</html>")
    }

    @Test
    fun ioExceptionMapsToNetwork() {
        val mapped = IOException("timeout").toApiException("t", "w")
        assertTrue(mapped is ApiException.Network)
        assertEquals("нет связи с сервером", mapped.userMessage("Ошибка").substringAfter(": "))
    }

    @Test
    fun serializationExceptionMapsToParse() {
        val mapped = SerializationException("bad").toApiException("t", "w")
        assertTrue(mapped is ApiException.Parse)
    }

    @Test
    fun httpErrorKeepsStatusCode() {
        val message = ApiException.Http(500).userMessage("Не удалось загрузить")
        assertEquals("Не удалось загрузить (HTTP 500)", message)
    }

    @Test
    fun unauthorizedHasSessionMessage() {
        assertEquals(
            "Не удалось загрузить: сессия истекла, войди заново",
            ApiException.Unauthorized().userMessage("Не удалось загрузить"),
        )
    }
}
