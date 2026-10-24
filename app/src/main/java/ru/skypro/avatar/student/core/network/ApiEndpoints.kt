package ru.skypro.avatar.student.core.network

object ApiEndpoints {
    const val ID_BASE = "https://id.skyeng.ru"
    const val EDU_AVATAR_BASE = "https://edu-avatar.skyeng.ru"

    const val LOGIN_PAGE = "$ID_BASE/login?redirect=https%3A%2F%2Favatar.skyeng.ru%2F"
    const val LOGIN_SUBMIT = "$ID_BASE/frame/login-submit"
    const val AUTH_JWT = "$ID_BASE/user-api/v1/auth/jwt"

    const val CABINET = "$EDU_AVATAR_BASE/api/v1/college-student-cabinet"
    const val TIMETABLE_WEEKLY = "$CABINET/timetable/weekly"
    const val TIMETABLE_DAY = "$CABINET/timetable/day"
    const val SUBJECTS = "$CABINET/student-account/subjects"
    const val SUBJECT_DETAIL = "$CABINET/single-student-account/profession"
    const val SCHOOL_SUBJECT = "https://edu-avatar.skyeng.ru/api/v2/college-student-cabinet/single-student-account/school-subject"
    const val ENGLISH_DETAIL = "https://edu-avatar.skyeng.ru/api/v3/college-student-cabinet/single-student-account/english"
    const val MATH_DETAIL = "$CABINET/single-student-account/math"
    const val CODE = "$EDU_AVATAR_BASE/api/v1/code"
    const val BRS_RATING = "$CABINET/brs-rating"
    const val DOCUMENTS = "$CABINET/student-account/documents"
    const val QUESTIONNAIRE = "$CABINET/student-account/profile-questionnaire"
    const val ACADEMIC_SUMMARY = "$CABINET/student-account/academic-summary"
    const val HELPFUL_LINKS = "$CABINET/student-account/helpful-links"
    const val EXTERNAL_SETTINGS = "https://student.skyeng.ru/profile"

    fun absolute(url: String): String = when {
        url.startsWith("http") -> url
        url.startsWith("//") -> "https:$url"
        else -> "$EDU_AVATAR_BASE/${url.trimStart('/')}"
    }
}
