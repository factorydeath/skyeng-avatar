package ru.skypro.avatar.student.data.subjects

import ru.skypro.avatar.student.core.network.dto.SubjectDto
import ru.skypro.avatar.student.domain.subjects.Subject

fun SubjectDto.toDomain(): Subject = Subject(
    code = subjectCode,
    lessonsDone = lessonProgress?.completed ?: 0,
    lessonsTotal = lessonProgress?.total ?: 0,
    debts = debts?.count ?: 0,
    blocked = isBlocked,
)
