package ru.skypro.avatar.student.domain.schedule

enum class SlotStatus { DONE, DEBTS, SOON, CUSTOM, MOBILE, NONE }

data class SlotAction(
    val title: String,
    val url: String,
    val isDebt: Boolean = false,
    val trainerTestId: String? = null,
)

data class Slot(
    val id: Long,
    val activity: String,
    val subjectCode: String,
    val title: String,
    val startsAtIso: String,
    val endsAtIso: String,
    val status: SlotStatus = SlotStatus.NONE,
    val statusText: String = "",
    val actions: List<SlotAction> = emptyList(),
    val timeZone: String = "",
)

data class DaySchedule(
    val date: String,
    val slots: List<Slot>,
    val timeZone: String = "",
)

data class WeekSchedule(
    val timeZone: String,
    val lunchLabel: String,
    val days: List<DaySchedule>,
)
