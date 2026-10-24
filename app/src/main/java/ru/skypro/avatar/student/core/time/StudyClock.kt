package ru.skypro.avatar.student.core.time

import io.ktor.client.HttpClient
import io.ktor.util.AttributeKey
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StudyClock(private val clock: Clock = Clock.systemUTC()) {
    @Volatile var zone: ZoneId = ZoneId.systemDefault()
        private set
    fun accept(timeZone: String) {
        runCatching { ZoneId.of(timeZone) }.getOrNull()?.let { zone = it }
    }
    fun today(): LocalDate = LocalDate.now(clock.withZone(zone))
}

private val clockKey = AttributeKey<StudyClock>("avatar.study.clock")
fun HttpClient.studyClock(): StudyClock = synchronized(this) {
    attributes.getOrNull(clockKey) ?: StudyClock().also { attributes.put(clockKey, it) }
}

fun scheduleTime(iso: String, timeZone: String): String {
    if (iso.isBlank()) return "—"
    val zone = runCatching { ZoneId.of(timeZone) }.getOrNull()
    val offset = runCatching { OffsetDateTime.parse(iso) }.getOrNull()
    if (offset != null) return (zone?.let { offset.atZoneSameInstant(it).toLocalTime() } ?: offset.toLocalTime())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    return runCatching { LocalDateTime.parse(iso).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("—")
}
