package ru.skypro.avatar.student.data.subjects

import kotlinx.coroutines.flow.Flow
import ru.skypro.avatar.student.domain.subjects.DetailSection
import ru.skypro.avatar.student.domain.subjects.Subject

interface SubjectsRepository {
    fun subjects(): Flow<List<Subject>>
    fun detailSections(subjectEnum: String): Flow<List<DetailSection>>
}
