package ru.skypro.avatar.student

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.data.trainer.KtorTrainerRepository
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.trainer.*

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerStagesTest {
    private val dispatcher = StandardTestDispatcher()
    private val clients = mutableListOf<HttpClient>()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { clients.forEach { it.close() }; Dispatchers.resetMain() }

    private fun vm(response: (String) -> Pair<Int, String>): TrainerViewModel {
        val client = HttpClient(MockEngine) {
            engine {
                dispatcher = this@TrainerStagesTest.dispatcher
                addHandler { request ->
                    val (code, body) = response(request.url.encodedPath.substringAfter("/code/"))
                    respond(body, HttpStatusCode.fromValue(code), headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json() }
        }
        clients += client
        return TrainerViewModel(KtorTrainerRepository(client), "test")
    }

    private fun TrainerViewModel.content() = (state.value as UiState.Content).data

    @Test fun repeatedStartAndFailedTaskGetDoNotRepeatPrepare() = runTest(dispatcher) {
        var prepares = 0
        var failTask = true
        val vm = vm { path -> when (path) {
            "test-prepare" -> { prepares++; 200 to """{"status":"ok","nextTaskId":"a"}""" }
            "get-task/a" -> if (failTask) 503 to "" else 200 to """{"id":"a","title":"Task"}"""
            else -> error(path)
        } }
        vm.start(); vm.start(); advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Error)
        failTask = false
        vm.retryStart(); advanceUntilIdle()
        vm.onAnswerChange("42")
        vm.start(); advanceUntilIdle()
        assertEquals(1, prepares)
        assertEquals("42", vm.content().answer)
    }

    @Test fun verdictRetryKeepsSubmissionAndSendsRunOnce() = runTest(dispatcher) {
        var runs = 0
        var failVerdict = true
        val vm = vm { path -> when (path) {
            "test-prepare" -> 200 to """{"status":"ok","nextTaskId":"a"}"""
            "get-task/a" -> 200 to """{"id":"a"}"""
            "run" -> { runs++; 200 to """{"submissionId":7}""" }
            "get-task-student/a" -> if (failVerdict) 503 to "" else 200 to """{"submissions":[{"id":7,"status":"success","score":1}]}"""
            else -> error(path)
        } }
        vm.start(); advanceUntilIdle()
        vm.onAnswerChange("42"); vm.check(); advanceUntilIdle()
        assertEquals(7L, vm.content().submissionId)
        assertTrue(vm.content().result is TrainerResult.Unavailable)
        failVerdict = false
        vm.check(); advanceUntilIdle()
        assertEquals(1, runs)
        assertEquals(TrainerResult.Correct, vm.content().result)
        assertNull(vm.content().submissionId)
    }

    @Test fun editAfterUnavailableClearsSubmissionAndResubmits() = runTest(dispatcher) {
        var runs = 0
        var verdicts = 0
        val vm = vm { path -> when (path) {
            "test-prepare" -> 200 to """{"status":"ok","nextTaskId":"a"}"""
            "get-task/a" -> 200 to """{"id":"a"}"""
            "run" -> { runs++; 200 to """{"submissionId":$runs}""" }
            "get-task-student/a" -> { verdicts++; if (verdicts == 1) 503 to "" else 200 to """{"submissions":[{"id":2,"status":"success","score":1}]}""" }
            else -> error(path)
        } }
        vm.start(); advanceUntilIdle()
        vm.onAnswerChange("42"); vm.check(); advanceUntilIdle()
        assertEquals(1L, vm.content().submissionId)
        assertTrue(vm.content().result is TrainerResult.Unavailable)
        vm.onAnswerChange("99")
        assertEquals("99", vm.content().answer)
        assertNull(vm.content().submissionId)
        assertNull(vm.content().result)
        vm.check(); advanceUntilIdle()
        assertEquals(2, runs)
        assertEquals(TrainerResult.Correct, vm.content().result)
        assertNull(vm.content().submissionId)
    }

    @Test fun rapidDoubleCheckSendsRunOnce() = runTest(dispatcher) {
        var runs = 0
        val vm = vm { path -> when (path) {
            "test-prepare" -> 200 to """{"status":"ok","nextTaskId":"a"}"""
            "get-task/a" -> 200 to """{"id":"a"}"""
            "run" -> { runs++; 200 to """{"submissionId":7}""" }
            "get-task-student/a" -> 200 to """{"submissions":[{"id":7,"status":"success","score":1}]}"""
            else -> error(path)
        } }
        vm.start(); advanceUntilIdle()
        vm.onAnswerChange("42")
        vm.check(); vm.check()
        advanceUntilIdle()
        assertEquals(1, runs)
        assertEquals(TrainerResult.Correct, vm.content().result)
    }

    @Test fun skipAndNextStayAvailableAfterUnavailableVerdict() = runTest(dispatcher) {
        val vm = vm { path -> when (path) {
            "test-prepare" -> 200 to """{"status":"ok","nextTaskId":"a"}"""
            "get-task/a" -> 200 to """{"id":"a"}"""
            "run" -> 200 to """{"submissionId":7}"""
            "get-task-student/a" -> 503 to ""
            "finish-task" -> 200 to """{"status":"all_tasks_finished"}"""
            else -> error(path)
        } }
        vm.start(); advanceUntilIdle()
        vm.onAnswerChange("42"); vm.check(); advanceUntilIdle()
        assertTrue(vm.content().result is TrainerResult.Unavailable)
        vm.requestSkip()
        assertTrue(vm.content().skipConfirmVisible)
        vm.dismissSkip()
        vm.next(); advanceUntilIdle()
        assertTrue(vm.content().finished)
        assertNull(vm.content().submissionId)
    }

    @Test fun transitionAndNextTaskRetriesNeverRepeatFinishPost() = runTest(dispatcher) {
        var finishes = 0
        var pollFail = true
        var taskFail = true
        var polls = 0
        val vm = vm { path -> when (path) {
            "test-prepare" -> 200 to """{"status":"ok","nextTaskId":"a"}"""
            "get-task/a" -> 200 to """{"id":"a"}"""
            "finish-task" -> { finishes++; 200 to """{"status":"calculation_in_progress","transitionId":13}""" }
            "next-task-status/13" -> { polls++; if (pollFail) 503 to "" else 200 to """{"status":"finished","taskId":"b"}""" }
            "get-task/b" -> if (taskFail) 503 to "" else 200 to """{"id":"b"}"""
            else -> error(path)
        } }
        vm.start(); advanceUntilIdle()
        vm.next(); advanceUntilIdle()
        assertNotNull(vm.content().pendingNext)
        pollFail = false
        vm.next(); advanceUntilIdle()
        val priorPolls = polls
        taskFail = false
        vm.next(); advanceUntilIdle()
        assertEquals(1, finishes)
        assertEquals(priorPolls, polls)
        assertEquals("b", vm.content().task?.id)
        assertNull(vm.content().pendingNext)
    }

    @Test fun completedPrepareShowsFinishedAndUnknownStatusShowsError() = runTest(dispatcher) {
        val done = vm { 200 to """{"status":"all_tasks_finished"}""" }
        done.start(); advanceUntilIdle()
        assertTrue(done.content().finished)
        val unknown = vm { 200 to """{"status":"unexpected","nextTaskId":"a"}""" }
        unknown.start(); advanceUntilIdle()
        assertTrue(unknown.state.value is UiState.Error)
    }

    @Test fun failedPostIsNotAutomaticallyRetried() = runTest(dispatcher) {
        var runs = 0
        val vm = vm { path -> when (path) {
            "test-prepare" -> 200 to """{"status":"ok","nextTaskId":"a"}"""
            "get-task/a" -> 200 to """{"id":"a"}"""
            "run" -> { runs++; 503 to "" }
            else -> error(path)
        } }
        vm.start(); advanceUntilIdle()
        vm.onAnswerChange("42"); vm.check(); advanceUntilIdle()
        assertEquals(1, runs)
        assertTrue(vm.content().result is TrainerResult.Unavailable)
    }
}
