package ru.skypro.avatar.student.data.aihelper

import ru.skypro.avatar.student.domain.trainer.ConditionPart

fun parseAiMessage(text: String): List<ConditionPart> {
    val parts = mutableListOf<ConditionPart>()
    val buf = StringBuilder()
    var i = 0
    fun flush() {
        if (buf.isNotEmpty()) {
            parts += ConditionPart.Text(buf.toString())
            buf.clear()
        }
    }
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 1 < text.length && (text[i + 1] == '$' || text[i + 1] == '\\')) {
            buf.append(text[i + 1])
            i += 2
            continue
        }
        if (c == '\\' && i + 1 < text.length && (text[i + 1] == '(' || text[i + 1] == '[')) {
            val close = if (text[i + 1] == '(') "\\)" else "\\]"
            val end = text.indexOf(close, i + 2)
            if (end >= 0) {
                flush()
                parts += ConditionPart.Formula(text.substring(i + 2, end))
                i = end + 2
                continue
            }
            buf.append(c)
            i++
            continue
        }
        if (c == '$') {
            val display = i + 1 < text.length && text[i + 1] == '$'
            val openLen = if (display) 2 else 1
            val end = findClose(text, i + openLen, display)
            if (end >= 0) {
                flush()
                parts += ConditionPart.Formula(text.substring(i + openLen, end).trim())
                i = end + openLen
                continue
            }
            buf.append(c)
            i++
            continue
        }
        buf.append(c)
        i++
    }
    flush()
    return parts
}

private fun findClose(text: String, from: Int, display: Boolean): Int {
    var i = from
    while (i < text.length) {
        val c = text[i]
        if (c == '\\') {
            i += 2
            continue
        }
        if (c == '$') {
            if (display) {
                if (i + 1 < text.length && text[i + 1] == '$') return i
                i++
                continue
            }
            if (i + 1 < text.length && text[i + 1] == '$') return -1
            return i
        }
        i++
    }
    return -1
}
