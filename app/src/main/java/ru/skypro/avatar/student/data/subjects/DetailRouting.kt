package ru.skypro.avatar.student.data.subjects

enum class DetailKind { PROFESSION, SCHOOL, ENGLISH, MATH }

private val SCHOOL_CODES = setOf(
    "russian", "geography", "physics", "chemistry", "biology", "basics_of_security",
)

fun detailKind(code: String): DetailKind = when (code) {
    "english" -> DetailKind.ENGLISH
    "math" -> DetailKind.MATH
    in SCHOOL_CODES -> DetailKind.SCHOOL
    else -> DetailKind.PROFESSION
}

fun lessonStatus(status: String?, score: Double?): String {
    val base = when (status) {
        "completed" -> "Пройдено"
        "in_progress", "opened" -> "В процессе"
        "failed" -> "Не сдан"
        null -> ""
        else -> status
    }
    return listOf(base, score?.let { "Балл $it" }).filter { !it.isNullOrEmpty() }.joinToString(" · ")
}

fun isLessonDebt(
    type: String?,
    title: String?,
    status: String?,
    passed: Boolean?,
    completedAt: String?,
    attended: Boolean?,
    watchedRecordCredited: Boolean?,
    deadlineAt: String?,
    nowIso: String,
): Boolean {
    if (type == "webinar") return false
    if ((title ?: "").contains("диагностическ", ignoreCase = true)) return false
    if (status == "completed" || passed == true || completedAt != null) return false
    if (type == "webinar" && (attended == true || watchedRecordCredited == true)) return false
    if (deadlineAt == null) return false
    return try {
        deadlineAt.take(10) < nowIso.take(10)
    } catch (e: Exception) {
        false
    }
}
