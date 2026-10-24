package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.skypro.avatar.student.domain.trainer.Submission
import ru.skypro.avatar.student.domain.trainer.TrainerVerdict
import ru.skypro.avatar.student.domain.trainer.trainerVerdict

class TrainerVerdictTest {
    @Test
    fun emptyListIsDefault() {
        assertEquals(TrainerVerdict.Default, trainerVerdict(emptyList()))
    }

    @Test
    fun successWithScoreOneIsSuccess() {
        val list = listOf(Submission(2, "success", 1.0, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.Success(2, 1.0), trainerVerdict(list))
    }

    @Test
    fun successWithoutScoreCountsAsFullScore() {
        val list = listOf(Submission(3, "success", null, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.Success(3, 1.0), trainerVerdict(list))
    }

    @Test
    fun newerErrorDoesNotCancelOlderSuccess() {
        val list = listOf(
            Submission(1, "success", 1.0, "2026-10-12T10:00:00+03:00"),
            Submission(2, "error", 0.0, "2026-10-12T11:00:00+03:00"),
        )
        assertTrue(trainerVerdict(list) is TrainerVerdict.Success)
    }

    @Test
    fun newestIsChosenByCreatedAtNotById() {
        val list = listOf(
            Submission(9, "error", 0.0, "2026-10-12T10:00:00+03:00"),
            Submission(5, "processing", null, "2026-10-12T12:00:00+03:00"),
        )
        assertEquals(TrainerVerdict.Processing(5), trainerVerdict(list))
    }

    @Test
    fun latestProcessingIsProcessing() {
        val list = listOf(Submission(7, "processing", null, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.Processing(7), trainerVerdict(list))
    }

    @Test
    fun latestZeroScoreIsError() {
        val list = listOf(Submission(4, "error", 0.0, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.Error(4, 0.0), trainerVerdict(list))
    }

    @Test
    fun latestPartialScoreIsPartial() {
        val list = listOf(Submission(6, "partial", 0.5, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.Partial(6, 0.5), trainerVerdict(list))
    }

    @Test
    fun invalidImageIsInvalidImage() {
        val list = listOf(Submission(8, "invalid_image", null, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.InvalidImage(8), trainerVerdict(list))
    }

    @Test
    fun gradingUnavailableIsGradingUnavailable() {
        val list = listOf(Submission(10, "grading_unavailable", null, "2026-10-12T10:00:00+03:00"))
        assertEquals(TrainerVerdict.GradingUnavailable(10), trainerVerdict(list))
    }
}
