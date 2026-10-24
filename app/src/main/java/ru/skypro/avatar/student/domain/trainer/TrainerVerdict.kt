package ru.skypro.avatar.student.domain.trainer

import java.time.OffsetDateTime

data class Submission(
    val id: Long,
    val status: String,
    val score: Double?,
    val createdAt: String,
)

sealed interface TrainerVerdict {
    data object Default : TrainerVerdict
    data class Success(val submissionId: Long, val score: Double) : TrainerVerdict
    data class Partial(val submissionId: Long, val score: Double) : TrainerVerdict
    data class Error(val submissionId: Long, val score: Double) : TrainerVerdict
    data class Processing(val submissionId: Long) : TrainerVerdict
    data class InvalidImage(val submissionId: Long) : TrainerVerdict
    data class GradingUnavailable(val submissionId: Long) : TrainerVerdict
}

private const val STATUS_PROCESSING = "processing"
private const val STATUS_SUCCESS = "success"
private const val STATUS_INVALID_IMAGE = "invalid_image"
private const val STATUS_GRADING_UNAVAILABLE = "grading_unavailable"

private fun scoreOf(s: Submission): Double? = when {
    s.status == STATUS_GRADING_UNAVAILABLE || s.status == STATUS_INVALID_IMAGE -> null
    s.score != null -> s.score
    s.status == STATUS_SUCCESS -> 1.0
    else -> 0.0
}

private fun epochOf(createdAt: String): Long? =
    runCatching { OffsetDateTime.parse(createdAt).toInstant().toEpochMilli() }.getOrNull()

private val newestFirst = Comparator<Submission> { a, b ->
    val ta = epochOf(a.createdAt)
    val tb = epochOf(b.createdAt)
    when {
        ta != null && tb != null && ta != tb -> tb.compareTo(ta)
        ta != null && tb == null -> -1
        ta == null && tb != null -> 1
        else -> a.id.compareTo(b.id)
    }
}

fun trainerVerdict(submissions: List<Submission>): TrainerVerdict {
    if (submissions.isEmpty()) return TrainerVerdict.Default
    val sorted = submissions.sortedWith(newestFirst)
    val best = sorted
        .mapNotNull { s -> scoreOf(s)?.takeIf { it >= 1.0 }?.let { s to it } }
        .fold(null as Pair<Submission, Double>?) { acc, candidate ->
            if (acc == null || acc.second < candidate.second) candidate else acc
        }
    if (best != null) return TrainerVerdict.Success(best.first.id, best.second)
    val latest = sorted.first()
    return when (latest.status) {
        STATUS_PROCESSING -> TrainerVerdict.Processing(latest.id)
        STATUS_INVALID_IMAGE -> TrainerVerdict.InvalidImage(latest.id)
        STATUS_GRADING_UNAVAILABLE -> TrainerVerdict.GradingUnavailable(latest.id)
        else -> {
            val score = scoreOf(latest)
            when {
                score == null -> TrainerVerdict.GradingUnavailable(latest.id)
                score > 0.0 -> TrainerVerdict.Partial(latest.id, score)
                else -> TrainerVerdict.Error(latest.id, score)
            }
        }
    }
}
