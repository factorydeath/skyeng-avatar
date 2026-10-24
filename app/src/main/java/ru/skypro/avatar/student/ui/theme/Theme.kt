package ru.skypro.avatar.student.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme { LIGHT, DARK, BLACK, SYSTEM }

@Composable
fun AvatarTheme(
    mode: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = when {
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }.let { base ->
        if (mode == AppTheme.BLACK) base.copy(background = Color.Black, surface = Color.Black) else base
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

fun subjectColor(code: String): Color = when (code) {
    "math" -> Color(0xFFF472B6)
    "russian" -> Color(0xFF60A5FA)
    "english" -> Color(0xFF34D399)
    "physics" -> Color(0xFFFBBF24)
    "chemistry" -> Color(0xFFA78BFA)
    "biology" -> Color(0xFF4ADE80)
    "geography" -> Color(0xFF22D3EE)
    "physical_education" -> Color(0xFFFB923C)
    "soft_skills" -> Color(0xFF8B5CF6)
    "basics_of_security" -> Color(0xFFF87171)
    else -> Color(0xFF9CA3AF)
}
