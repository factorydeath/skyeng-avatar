package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.skypro.avatar.student.data.subjects.isLessonDebt
import ru.skypro.avatar.student.data.subjects.lessonStatus
import ru.skypro.avatar.student.ui.components.ellipsizeWords

class DebtAndTextTest {
    private val now = "2026-10-10"

    @Test
    fun overdueUncompletedLessonIsDebt() {
        assertTrue(
            isLessonDebt("lesson", "Урок 1", "opened", false, null, false, false, "2026-10-01", now),
        )
    }

    @Test
    fun webinarIsNeverDebt() {
        assertFalse(
            isLessonDebt("webinar", "Вебинар", "opened", false, null, false, false, "2026-10-01", now),
        )
    }

    @Test
    fun diagnosticTestIsNeverDebt() {
        assertFalse(
            isLessonDebt("lesson", "Диагностический тест", "opened", false, null, false, false, "2026-10-01", now),
        )
    }

    @Test
    fun completedLessonIsNotDebt() {
        assertFalse(
            isLessonDebt("lesson", "Урок", "completed", true, "2026-10-02", false, false, "2026-10-01", now),
        )
    }

    @Test
    fun lessonWithoutDeadlineIsNotDebt() {
        assertFalse(
            isLessonDebt("lesson", "Урок", "opened", false, null, false, false, null, now),
        )
    }

    @Test
    fun deadlineInFutureIsNotDebt() {
        assertFalse(
            isLessonDebt("lesson", "Урок", "opened", false, null, false, false, "2026-10-20", now),
        )
    }

    @Test
    fun lessonStatusJoinsScore() {
        assertEquals("Пройдено · Балл 5.0", lessonStatus("completed", 5.0))
        assertEquals("", lessonStatus(null, null))
    }

    @Test
    fun shortTitleIsUnchanged() {
        assertEquals("Пройти тест", ellipsizeWords("Пройти тест", 14))
    }

    @Test
    fun longTitleIsCutByWordWithEllipsis() {
        assertEquals("Пройти тест...", ellipsizeWords("Пройти тест по физике", 14))
    }

    @Test
    fun singleLongWordIsCutWithEllipsis() {
        assertEquals("Абвгдежзийклмноп...", ellipsizeWords("Абвгдежзийклмнопрст", 16))
    }
}
