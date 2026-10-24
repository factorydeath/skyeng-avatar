package ru.skypro.avatar.student.ui.navigation

object Routes {
    const val MY_DAY = "my_day"
    const val SCHEDULE = "schedule"
    const val SUBJECTS = "subjects"
    const val SUBJECT_DETAIL = "subject_detail/{code}"
    const val DOCUMENTS = "documents"
    const val PROFILE = "profile"
    const val TRAINER = "trainer/{testId}"

    fun subjectDetail(code: String) = "subject_detail/$code"
    fun trainer(testId: String) = "trainer/$testId"
}
