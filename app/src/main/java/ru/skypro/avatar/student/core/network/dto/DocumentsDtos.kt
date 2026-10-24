package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class StudentDocumentDto(
    val id: Long = 0L,
    val type: String = "",
    val number: String = "",
    val status: String = "",
    val createdAt: String = "",
)

@Serializable
data class DocumentsResponseDto(
    val types: List<String> = emptyList(),
    val documents: List<StudentDocumentDto> = emptyList(),
    val availableTypes: List<String> = emptyList(),
)

@Serializable
data class DocumentDownloadDto(val signedUrl: String = "")
