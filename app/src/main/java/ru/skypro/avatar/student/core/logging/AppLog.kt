package ru.skypro.avatar.student.core.logging

object AppLog {
    const val AUTH = "AvatarAuth"
    const val SESSION = "AvatarSession"
    const val SCHEDULE = "AvatarSchedule"
    const val SUBJECTS = "AvatarSubjects"
    const val DAY = "AvatarDay"
    const val DOCS = "AvatarDocs"
    const val PROFILE = "AvatarProfile"
    const val UI = "AvatarUI"
    const val TRAINER = "AvatarTrainer"

    var printer: (level: Int, tag: String, msg: String) -> Unit = { _, tag, msg ->
        println("[$tag] $msg")
    }

    fun d(tag: String, msg: String) = printer(3, tag, msg)
    fun i(tag: String, msg: String) = printer(4, tag, msg)
    fun w(tag: String, msg: String, e: Throwable? = null) =
        printer(5, tag, if (e == null) msg else "$msg | ${e::class.simpleName}")

    fun e(tag: String, msg: String, e: Throwable? = null) =
        printer(6, tag, if (e == null) msg else "$msg | ${e::class.simpleName}")
}
