package ru.skypro.avatar.student.data.schedule

import ru.skypro.avatar.student.core.network.dto.DayScheduleDto
import ru.skypro.avatar.student.core.network.dto.SlotDto
import ru.skypro.avatar.student.core.network.dto.WeekScheduleDto
import ru.skypro.avatar.student.domain.schedule.DaySchedule
import ru.skypro.avatar.student.domain.schedule.Slot
import ru.skypro.avatar.student.domain.schedule.SlotAction
import ru.skypro.avatar.student.domain.schedule.SlotStatus
import ru.skypro.avatar.student.domain.schedule.WeekSchedule

fun SlotDto.toDomain(timeZone: String = ""): Slot {
    val ctx = context
    val actions = mutableListOf<SlotAction>()
    ctx?.actions?.forEach { a ->
        if (!a.locked) {
            (a.openTarget?.url ?: a.url)?.takeIf { it.isNotEmpty() }?.let {
                val testId = if (a.kind == "test-prep") TEST_ID.find(it)?.value else null
                actions += SlotAction(a.title, it, a.kind.startsWith("debt"), testId)
            }
        }
    }
    ctx?.webinar?.recordingUrl?.takeIf { it.isNotEmpty() }?.let {
        actions += SlotAction("Запись", it)
    }
    ctx?.webinar?.joinUrl?.takeIf { it.isNotEmpty() && actions.none { a -> a.title == "Запись" } }?.let {
        actions += SlotAction("Подключиться", it)
    }
    ctx?.test?.joinUrl?.takeIf { it.isNotEmpty() }?.let {
        actions += SlotAction("Пройти тест", it)
    }
    val (status, text) = when (ctx?.state) {
        "all-done" -> SlotStatus.DONE to "Пройдено"
        "has-actions" -> SlotStatus.DEBTS to "Долги ${ctx.debtsCount}"
        "not-open-yet" -> SlotStatus.SOON to "Скоро"
        "custom" -> SlotStatus.CUSTOM to (ctx.actions.firstOrNull()?.title ?: "")
        "mobile-app" -> SlotStatus.MOBILE to "В приложении"
        else -> SlotStatus.NONE to ""
    }
    return Slot(
        id = slotId,
        activity = activityType,
        subjectCode = subject ?: "",
        title = title ?: ownTitle ?: "",
        startsAtIso = startsAt,
        endsAtIso = endsAt,
        status = status,
        statusText = text,
        actions = actions,
        timeZone = timeZone,
    )
}

private val TEST_ID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

fun DayScheduleDto.toDomain(): DaySchedule =
    DaySchedule(date = date, slots = slots.map { it.toDomain(timezone) }, timeZone = timezone)

fun WeekScheduleDto.toDomain(): WeekSchedule {
    val mapped = days.map { DaySchedule(date = it.date, slots = it.slots.map { s -> s.toDomain(timezone) }, timeZone = timezone) }
    val withSaturday = try {
        val last = mapped.lastOrNull()?.date?.take(10)
        if (last != null && java.time.LocalDate.parse(last).dayOfWeek == java.time.DayOfWeek.FRIDAY) {
            mapped + DaySchedule(java.time.LocalDate.parse(last).plusDays(1).toString(), emptyList())
        } else mapped
    } catch (e: Exception) {
        mapped
    }
    return WeekSchedule(
        timeZone = timezone,
        lunchLabel = listOfNotNull(lunch?.start, lunch?.end).joinToString("–"),
        days = withSaturday,
    )
}
