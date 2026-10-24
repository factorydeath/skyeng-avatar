package ru.skypro.avatar.student.domain.subjects

data class Subject(
    val code: String,
    val lessonsDone: Int,
    val lessonsTotal: Int,
    val debts: Int,
    val blocked: Boolean,
)

data class DetailItem(
    val title: String,
    val status: String = "",
    val openUrl: String = "",
    val isDebt: Boolean = false,
)

data class DetailSection(
    val title: String,
    val items: List<DetailItem> = emptyList(),
)
