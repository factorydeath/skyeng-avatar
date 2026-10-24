package ru.skypro.avatar.student.data.trainer

import ru.skypro.avatar.student.domain.trainer.ConditionPart

private val mathRegex = Regex("<vim-math>(.*?)</vim-math>", RegexOption.DOT_MATCHES_ALL)
private val imageRegex = Regex("<img[^>]*?src=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
private val blockEnd = Regex("(?i)</p>|<br\\s*/?>|</h\\d>")
private val tagRegex = Regex("<[^>]+>")
private val spaceRegex = Regex("[ \\t\\r\\u00A0]+")
private val entities = mapOf(
    "&nbsp;" to " ",
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
)

fun parseCondition(html: String): List<ConditionPart> {
    val parts = mutableListOf<ConditionPart>()
    var last = 0
    val tokens = Regex("${mathRegex.pattern}|${imageRegex.pattern}", RegexOption.DOT_MATCHES_ALL)
    tokens.findAll(html).forEach { m ->
        addText(parts, html.substring(last, m.range.first))
        val math = m.groups[1]
        val image = m.groups[2]
        when {
            math != null -> parts += ConditionPart.Formula(math.value.trim())
            image != null -> parts += ConditionPart.Image(image.value.trim())
        }
        last = m.range.last + 1
    }
    addText(parts, html.substring(last))
    return parts
}

fun List<ConditionPart>.paragraphs(): List<List<ConditionPart>> {
    val result = mutableListOf<List<ConditionPart>>()
    var current = mutableListOf<ConditionPart>()
    fun endParagraph() {
        if (current.isNotEmpty()) {
            result += current
            current = mutableListOf()
        }
    }
    forEach { part ->
        when (part) {
            is ConditionPart.Formula -> current += part
            is ConditionPart.Image -> {
                endParagraph()
                result += listOf(part)
            }
            is ConditionPart.Text -> part.value.split("\n").forEachIndexed { i, line ->
                if (i > 0) endParagraph()
                if (line.isNotBlank()) current += ConditionPart.Text(line)
            }
        }
    }
    endParagraph()
    return result
}

private fun addText(parts: MutableList<ConditionPart>, raw: String) {
    var text = tagRegex.replace(blockEnd.replace(raw, "\n"), "")
    entities.forEach { (from, to) -> text = text.replace(from, to) }
    text = spaceRegex.replace(text, " ").trim('\n')
    if (text.isNotEmpty()) parts += ConditionPart.Text(text)
}
