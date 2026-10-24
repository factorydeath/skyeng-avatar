package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.skypro.avatar.student.data.aihelper.parseAiMessage
import ru.skypro.avatar.student.domain.trainer.ConditionPart

class AiMessageParserTest {
    @Test
    fun inlineDollarParsed() {
        val parts = parseAiMessage("Это $100\\%$ верно")
        assertEquals(3, parts.size)
        assertEquals(ConditionPart.Text("Это "), parts[0])
        assertEquals(ConditionPart.Formula("100\\%"), parts[1])
        assertEquals(ConditionPart.Text(" верно"), parts[2])
    }

    @Test
    fun displayDollarsParsed() {
        val parts = parseAiMessage("Формула: " + "\$\$x^2+1\$\$" + " конец")
        assertEquals(3, parts.size)
        assertEquals(ConditionPart.Formula("x^2+1"), parts[1])
    }

    @Test
    fun parenBracketsParsed() {
        val parts = parseAiMessage("\\(a+b\\) и \\[c\\]")
        assertEquals(3, parts.size)
        assertEquals(ConditionPart.Formula("a+b"), parts[0])
        assertEquals(ConditionPart.Text(" и "), parts[1])
        assertEquals(ConditionPart.Formula("c"), parts[2])
    }

    @Test
    fun lonelyDollarStaysText() {
        val parts = parseAiMessage("стоит " + "\$5" + " всего")
        assertEquals(1, parts.size)
        assertTrue(parts[0] is ConditionPart.Text)
        assertEquals("стоит \$5 всего", (parts[0] as ConditionPart.Text).value)
    }

    @Test
    fun escapedDollarStaysText() {
        val parts = parseAiMessage("скидка " + "\\\$10" + " и " + "\$x\$")
        assertEquals(2, parts.size)
        assertEquals(ConditionPart.Text("скидка $10 и "), parts[0])
        assertEquals(ConditionPart.Formula("x"), parts[1])
    }
}
