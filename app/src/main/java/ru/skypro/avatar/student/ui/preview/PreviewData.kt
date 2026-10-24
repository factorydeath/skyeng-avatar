package ru.skypro.avatar.student.ui.preview

import ru.skypro.avatar.student.domain.schedule.Slot
import ru.skypro.avatar.student.domain.subjects.Subject

val previewSlots = listOf(
    Slot(1735, "lesson", "math", "Тренажёр 3", "2026-10-09T10:20:00+03:00", "2026-10-09T11:05:00+03:00"),
    Slot(1736, "webinar", "physics", "Вебинар", "2026-10-09T11:10:00+03:00", "2026-10-09T11:55:00+03:00"),
)

val previewSubjects = listOf(
    Subject("math", 4, 10, 3, false),
    Subject("soft_skills", 5, 5, 0, false),
)
