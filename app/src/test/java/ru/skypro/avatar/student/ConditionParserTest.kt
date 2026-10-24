package ru.skypro.avatar.student

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.skypro.avatar.student.data.trainer.paragraphs
import ru.skypro.avatar.student.data.trainer.parseCondition
import ru.skypro.avatar.student.domain.trainer.ConditionPart

class ConditionParserTest {
    @Test
    fun extractsFormulaAndKeepsSurroundingText() {
        val html = "<p>Прошли 25<vim-math>\\%</vim-math> всего маршрута.</p>"
        assertEquals(
            listOf(
                ConditionPart.Text("Прошли 25"),
                ConditionPart.Formula("\\%"),
                ConditionPart.Text(" всего маршрута."),
            ),
            parseCondition(html),
        )
    }

    @Test
    fun decodesEntitiesAndStripsTags() {
        val parts = parseCondition("<p><strong>Ответ&nbsp;&amp;</strong> ok</p>")
        assertEquals(listOf(ConditionPart.Text("Ответ & ok")), parts)
    }

    @Test
    fun paragraphsSplitOnBlockBoundaries() {
        val html = "<p>Первая</p>\n<p>Вторая <vim-math>x^2</vim-math></p>"
        val paragraphs = parseCondition(html).paragraphs()
        assertEquals(2, paragraphs.size)
        assertEquals(ConditionPart.Text("Первая"), paragraphs[0].single())
        assertEquals(
            listOf(ConditionPart.Text("Вторая "), ConditionPart.Formula("x^2")),
            paragraphs[1],
        )
    }
}
