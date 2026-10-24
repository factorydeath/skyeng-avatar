package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ru.skypro.avatar.student.core.auth.LoginValidator
import ru.skypro.avatar.student.data.subjects.DetailKind
import ru.skypro.avatar.student.data.subjects.detailKind
import ru.skypro.avatar.student.data.subjects.isLessonDebt

class LoginValidationTest {
    @Test
    fun email() {
        assertNull(LoginValidator.emailError("a@b.ru"))
        assertNotNull(LoginValidator.emailError("abc"))
    }

    @Test
    fun password() {
        assertNull(LoginValidator.passwordError("123456"))
        assertNotNull(LoginValidator.passwordError("123"))
    }

    @Test
    fun edgeSpacesAreTrimmedBeforeValidation() {
        assertNull(LoginValidator.emailError("  a@b.ru  ".trim()))
        assertNull(LoginValidator.passwordError("  123456 ".trim()))
        assertNotNull(LoginValidator.passwordError("      ".trim()))
    }

    @Test
    fun routing() {
        assertEquals(DetailKind.PROFESSION, detailKind("soft_skills"))
        assertEquals(DetailKind.PROFESSION, detailKind("career_guidance_professions"))
        assertEquals(DetailKind.SCHOOL, detailKind("russian"))
        assertEquals(DetailKind.SCHOOL, detailKind("basics_of_security"))
        assertEquals(DetailKind.ENGLISH, detailKind("english"))
        assertEquals(DetailKind.MATH, detailKind("math"))
    }

    @Test
    fun webinarDebt() {
        assertEquals(false, isLessonDebt("webinar", "Вебинар", null, false, null, true, false, "2026-09-07", "2026-10-09"))
        assertEquals(false, isLessonDebt("webinar", "Вебинар", null, false, null, false, false, "2026-09-07", "2026-10-09"))
        assertEquals(false, isLessonDebt("test", "Диагностический тест №1", null, false, null, null, null, "2026-09-07", "2026-10-09"))
        assertEquals(true, isLessonDebt("test", "Тест. Неравенства", null, false, null, null, null, "2026-09-07", "2026-10-09"))
        assertEquals(false, isLessonDebt("test", "Тест", null, false, null, null, null, "2026-12-01", "2026-10-09"))
    }
}
