package ru.skypro.avatar.student.domain.day

data class BrsSummary(
    val totalEarned: Double,
    val totalPlanned: Double,
    val totalAvailable: Double = 0.0,
    val percent: Int,
    val academicEarned: Double,
    val academicPlanned: Double,
    val disciplinaryCurrent: Int,
    val disciplinaryInitial: Int,
    val contribution: Int,
    val weeks: List<BrsWeek> = emptyList(),
    val subjects: List<BrsSubject> = emptyList(),
)

data class BrsWeek(
    val week: Int,
    val earned: Double,
    val available: Double,
)

data class BrsSubject(
    val id: String,
    val title: String,
    val earned: Double,
    val available: Double,
)

fun BrsSubject.percent(): Int =
    if (available <= 0) 0 else ((earned / available) * 100).toInt()

fun formatScore(value: Double): String {
    if (value == 0.0) return "0"
    val rounded = java.math.BigDecimal.valueOf(value).setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()
    return rounded.replace('.', ',')
}

fun formatPlanned(value: Double): String = formatScore(value)

fun brsChartMaximum(weeks: List<BrsWeek>): Double =
    weeks.maxOfOrNull { maxOf(it.available, it.earned) }?.coerceAtLeast(1.0) ?: 1.0

fun brsChartFraction(value: Double, maximum: Double): Float =
    if (!value.isFinite() || !maximum.isFinite() || maximum <= 0) 0f else (value / maximum).coerceIn(0.0, 1.0).toFloat()
