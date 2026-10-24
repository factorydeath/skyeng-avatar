package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import ru.skypro.avatar.student.ui.screens.dayPhraseFor
import java.time.LocalDate

class DayPhraseTest {
    @Test
    fun saturdayShowsRestPhrase() {
        assertEquals("Суббота — день для отдыха 🌿", dayPhraseFor(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun weekdayDoesNotShowRestPhrase() {
        assertNotEquals("Суббота — день для отдыха 🌿", dayPhraseFor(LocalDate.of(2026, 10, 12)))
    }
}
