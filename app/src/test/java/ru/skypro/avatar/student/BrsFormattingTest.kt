package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.skypro.avatar.student.domain.day.BrsSubject
import ru.skypro.avatar.student.domain.day.formatPlanned
import ru.skypro.avatar.student.domain.day.formatScore
import ru.skypro.avatar.student.domain.day.percent
import ru.skypro.avatar.student.ui.profile.BRS_SUBJECTS_PAGE_SIZE
import ru.skypro.avatar.student.ui.profile.pageCount
import ru.skypro.avatar.student.ui.profile.pageItems

class BrsFormattingTest {
    @Test
    fun scoreOneDecimalComma() {
        assertEquals("276,2", formatScore(276.17))
        assertEquals("81,2", formatScore(81.17))
        assertEquals("9,0", formatScore(9.01))
        assertEquals("11,8", formatScore(11.8))
        assertEquals("6,7", formatScore(6.65))
        assertEquals("9,4", formatScore(9.44))
        assertEquals("0", formatScore(0.0))
    }

    @Test
    fun plannedKeepsServerPrecision() {
        assertEquals("999,4", formatPlanned(999.4))
        assertEquals("799,4", formatPlanned(799.4))
        assertEquals("11,8", formatPlanned(11.8))
    }

    @Test
    fun subjectPercentMatchesSite() {
        assertEquals(76, BrsSubject("biology", "Биология", 9.01, 11.8).percent())
        assertEquals(67, BrsSubject("russian", "Русский язык", 11.24, 16.74).percent())
        assertEquals(70, BrsSubject("geography", "География", 6.65, 9.44).percent())
        assertEquals(68, BrsSubject("chemistry", "Химия", 8.08, 11.8).percent())
        assertEquals(0, BrsSubject("x", "X", 5.0, 0.0).percent())
    }

    @Test
    fun subjectsPagedByFive() {
        val all = (1..10).map { BrsSubject("id$it", "S$it", it.toDouble(), 10.0) }
        assertEquals(2, pageCount(all.size, BRS_SUBJECTS_PAGE_SIZE))
        assertEquals(listOf("S1", "S2", "S3", "S4", "S5"), all.pageItems(0, BRS_SUBJECTS_PAGE_SIZE).map { it.title })
        assertEquals(listOf("S6", "S7", "S8", "S9", "S10"), all.pageItems(1, BRS_SUBJECTS_PAGE_SIZE).map { it.title })
        assertEquals(0, all.pageItems(2, BRS_SUBJECTS_PAGE_SIZE).size)
        assertEquals(0, emptyList<BrsSubject>().pageItems(0, BRS_SUBJECTS_PAGE_SIZE).size)
        assertEquals(0, pageCount(0, BRS_SUBJECTS_PAGE_SIZE))
    }
}
