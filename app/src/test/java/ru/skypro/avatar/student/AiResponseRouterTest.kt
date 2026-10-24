package ru.skypro.avatar.student

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.data.aihelper.*

@OptIn(ExperimentalCoroutinesApi::class)
class AiResponseRouterTest {
    @Test fun jwtFailureFailsAuthorizationAndPendingInit() = runTest {
        val router = AiResponseRouter()
        val auth = async(start = CoroutineStart.UNDISPATCHED) { runCatching { router.awaitAuthorization(1000) } }
        router.receive(WsEnvelope(AiWsType.JWT_ERROR))
        assertTrue(auth.await().exceptionOrNull() is ru.skypro.avatar.student.core.network.ApiException.Unauthorized)
        val init = runCatching { router.request(1000, null, { true }) { fail("Must not send after JWT failure") } }
        assertTrue(init.exceptionOrNull() is ru.skypro.avatar.student.core.network.ApiException.Unauthorized)
    }

    @Test fun authorizationTimeoutDoesNotInitialize() = runTest {
        val router = AiResponseRouter()
        var initialized = false
        val result = runCatching { router.awaitAuthorization(1000); initialized = true }
        assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
        assertFalse(initialized)
        assertEquals(1000L, testScheduler.currentTime)
    }

    @Test fun immediateResponseDuringSendIsNotLost() = runTest {
        val router = AiResponseRouter()
        val expected = WsEnvelope(AiWsType.INIT_RESPONSE)
        val actual = router.request(1000, null, { it.type == AiWsType.INIT_RESPONSE }) {
            router.receive(expected)
        }
        assertEquals(expected, actual)
    }

    @Test fun initializationWaitsForAuthorizationEvenIfAuthorizationArrivesEarly() = runTest {
        val router = AiResponseRouter()
        var initialized = false
        val operation = launch(start = CoroutineStart.UNDISPATCHED) {
            router.awaitAuthorization(1000)
            initialized = true
        }
        assertFalse(initialized)
        router.receive(WsEnvelope(AiWsType.CONNECTED))
        assertFalse(initialized)
        router.receive(WsEnvelope(AiWsType.JWT_SUCCESS))
        operation.join()
        assertTrue(initialized)
        router.awaitAuthorization(1000)
    }

    @Test fun disconnectImmediatelyFailsAllPendingAndFutureOperations() = runTest {
        val router = AiResponseRouter()
        val failure = IOException("disconnected")
        val authorization = async(start = CoroutineStart.UNDISPATCHED) { runCatching { router.awaitAuthorization(68_000) } }
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { router.request(68_000, "a", { true }) {} }
        }
        router.fail(failure)
        assertTrue(authorization.await().exceptionOrNull() is IOException)
        assertEquals("disconnected", request.await().exceptionOrNull()?.message)
        var sent = false
        val future = runCatching { router.request(1000, "b", { true }) { sent = true } }
        assertSame(failure, future.exceptionOrNull())
        assertFalse(sent)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun canceledWaitDoesNotConsumeNextResponse() = runTest {
        val router = AiResponseRouter()
        val canceled = launch(start = CoroutineStart.UNDISPATCHED) { router.request(1000, null, { true }) {} }
        canceled.cancelAndJoin()
        val next = router.request(1000, null, { it.type == "next" }) { router.receive(WsEnvelope("next")) }
        assertEquals("next", next.type)
    }

    @Test fun responsesAndServerErrorsAreCorrelated() = runTest {
        val router = AiResponseRouter()
        val waiting = async(start = CoroutineStart.UNDISPATCHED) {
            router.request(1000, "current", { it.type == "expected" }) {}
        }
        router.receive(parseEnvelope("""{"type":"from_server__internal_error","data":{"messageId":"old","canRetry":true}}"""))
        router.receive(WsEnvelope("unrelated"))
        assertFalse(waiting.isCompleted)
        router.receive(WsEnvelope("expected"))
        assertEquals("expected", waiting.await().type)
    }

    @Test fun oldConnectionCannotDeliverIntoNewRouter() = runTest {
        val old = AiResponseRouter()
        val fresh = AiResponseRouter()
        val pending = async(start = CoroutineStart.UNDISPATCHED) { fresh.request(1000, null, { true }) {} }
        old.fail(IOException("closed"))
        old.receive(WsEnvelope("old"))
        assertFalse(pending.isCompleted)
        fresh.receive(WsEnvelope("fresh"))
        assertEquals("fresh", pending.await().type)
    }
}
