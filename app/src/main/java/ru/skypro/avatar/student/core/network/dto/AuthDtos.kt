package ru.skypro.avatar.student.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class StudentConfigDto(
    val config: TimeZoneDto,
    val currentTime: String,
)

@Serializable
data class TimeZoneDto(val timeZone: String)

@Serializable
data class PaymentStatusDto(val kind: String)

@Serializable
data class PhysicalDocumentRequestsDto(
    val types: List<String>,
    val csrfToken: String = "",
    val requests: List<String> = emptyList(),
)

@Serializable
data class JwtCheckDto(val success: Boolean)
