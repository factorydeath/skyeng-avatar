package ru.skypro.avatar.student

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.skypro.avatar.student.data.aihelper.AI_JSON
import ru.skypro.avatar.student.data.aihelper.AiWsType
import ru.skypro.avatar.student.data.aihelper.FeedbackPayload
import ru.skypro.avatar.student.data.aihelper.InitResponseDto
import ru.skypro.avatar.student.data.aihelper.SendTextPayload
import ru.skypro.avatar.student.data.aihelper.ServerErrorDto
import ru.skypro.avatar.student.data.aihelper.TextResponseDto
import ru.skypro.avatar.student.data.aihelper.WsEnvelope
import ru.skypro.avatar.student.data.aihelper.decodeData
import ru.skypro.avatar.student.data.aihelper.messages
import ru.skypro.avatar.student.data.aihelper.parseEnvelope

class AiHelperParsingTest {
    @Test
    fun initResponseHistoryParsed() {
        val raw = """{"type":"from_server__init_response","data":{"history":[
            {"itemId":"m1","message":"Привет!","createdAt":"2026-10-12","isFromUser":false,"like":null,"replyOnMessageId":null},
            {"itemId":"m2","message":"Помоги","createdAt":"2026-10-12","isFromUser":true,"like":null,"replyOnMessageId":null}
        ]}}"""
        val env = parseEnvelope(raw)
        assertEquals(AiWsType.INIT_RESPONSE, env.type)
        val messages = env.decodeData<InitResponseDto>().messages()
        assertEquals(2, messages.size)
        assertEquals("m1", messages[0].itemId)
        assertEquals("Привет!", messages[0].text)
        assertTrue(!messages[0].isFromUser)
        assertTrue(messages[1].isFromUser)
        assertNull(messages[0].like)
    }

    @Test
    fun textResponseParsed() {
        val raw = """{"type":"from_server__text_response","data":{"replyOnMessageId":"q1","itemId":"m9","text":"Попробуй так"}}"""
        val env = parseEnvelope(raw)
        assertEquals(AiWsType.TEXT_RESPONSE, env.type)
        val dto = env.decodeData<TextResponseDto>()
        assertEquals("q1", dto.replyOnMessageId)
        assertEquals("m9", dto.itemId)
        assertEquals("Попробуй так", dto.text)
    }

    @Test
    fun internalErrorParsed() {
        val raw = """{"type":"from_server__internal_error","data":{"messageId":"q1","canRetry":true,"errorKey":"llm_timeout","errorClass":"LlmError","errorMessage":"timeout"}}"""
        val env = parseEnvelope(raw)
        assertEquals(AiWsType.INTERNAL_ERROR, env.type)
        val dto = env.decodeData<ServerErrorDto>()
        assertEquals("q1", dto.messageId)
        assertTrue(dto.canRetry)
        assertEquals("timeout", dto.errorMessage)
    }

    @Test
    fun sendTextPayloadSerialized() {
        val raw = AI_JSON.encodeToString(
            WsEnvelope(
                AiWsType.SEND_TEXT,
                AI_JSON.encodeToJsonElement(
                    SendTextPayload.serializer(),
                    SendTextPayload(messageId = "id1", message = "как решить?"),
                ) as kotlinx.serialization.json.JsonObject,
            ),
        )
        assertTrue("raw=$raw", raw.contains("\"type\":\"from_client__send_text\""))
        assertTrue("raw=$raw", raw.contains("\"message\":\"как решить?\""))
        assertTrue("raw=$raw", raw.contains("\"scenario\":\"math_default\""))
    }

    @Test
    fun feedbackPayloadSerialized() {
        val raw = AI_JSON.encodeToString(
            FeedbackPayload(messageId = "f1", itemId = "m9", like = true),
        )
        assertTrue(raw.contains("\"itemId\":\"m9\""))
        assertTrue(raw.contains("\"like\":true"))
    }
}
