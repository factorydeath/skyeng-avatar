package ru.skypro.avatar.student

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.data.day.KtorDayExtrasRepository
import ru.skypro.avatar.student.data.profile.KtorProfileRepository
import ru.skypro.avatar.student.ui.profile.ProfileViewModel
import ru.skypro.avatar.student.ui.state.UiState

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileIndependenceTest {
    private val dispatcher = StandardTestDispatcher()
    private var http: HttpClient? = null
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { http?.close(); Dispatchers.resetMain() }

    private fun vm(brs: suspend () -> String): ProfileViewModel {
        val client = HttpClient(MockEngine) {
            engine {
                dispatcher = this@ProfileIndependenceTest.dispatcher
                addHandler { request ->
                    if (request.url.encodedPath.endsWith("brs-rating")) respond(brs())
                    else respond("", HttpStatusCode.Forbidden)
                }
            }
        }
        http = client
        return ProfileViewModel(KtorProfileRepository(client), KtorDayExtrasRepository(client))
    }

    private fun rating(earned: Int) = """{"availability":"available","rating":{"currentTotal":{"earned":$earned,"planned":100},"disciplinary_points":{"current":0,"initial":0}}}"""

    @Test fun profileFailureDoesNotHideSuccessfulBrs() = runTest(dispatcher) {
        val vm = vm { rating(42) }
        vm.load(); advanceUntilIdle()
        assertTrue(vm.summary.value is UiState.Error)
        assertEquals(42.0, (vm.brs.value as UiState.Content).data!!.totalEarned, 0.0)
    }

    @Test fun repeatedBrsLoadCancelsOldRequest() = runTest(dispatcher) {
        var requests = 0
        var canceled = false
        val vm = vm {
            requests++
            if (requests == 1) {
                try { delay(10_000); rating(1) } finally { canceled = true }
            } else rating(2)
        }
        vm.retryBrs(); runCurrent()
        vm.retryBrs(); advanceUntilIdle()
        assertTrue(canceled)
        assertEquals(2.0, (vm.brs.value as UiState.Content).data!!.totalEarned, 0.0)
    }
}
