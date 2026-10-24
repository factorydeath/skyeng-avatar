package ru.skypro.avatar.student.domain.trainer

data class TrainerTask(
    val id: String,
    val title: String,
    val condition: List<ConditionPart>,
)

sealed interface ConditionPart {
    data class Text(val value: String) : ConditionPart
    data class Formula(val latex: String) : ConditionPart
    data class Image(val url: String) : ConditionPart
}

sealed interface NextTaskResult {
    data class Pending(val transitionId: String) : NextTaskResult
    data class Found(val taskId: String) : NextTaskResult
    data object SkipConfirmationRequired : NextTaskResult
    data object NoTasksLeft : NextTaskResult
}
