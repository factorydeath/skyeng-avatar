package ru.skypro.avatar.student.core.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URI

fun externalWebUrl(raw: String): String? = runCatching {
    val value = raw.trim()
    if (value.isEmpty() || value.any { it <= ' ' || it == '\\' }) return null
    val uri = URI("https://avatar.skyeng.ru/").resolve(URI(value)).normalize()
    if (uri.scheme.lowercase() !in setOf("https", "http") || uri.host.isNullOrBlank() || uri.userInfo != null) return null
    uri.toASCIIString()
}.getOrNull()

fun openExternal(context: Context, raw: String) {
    val url = externalWebUrl(raw)
    val error = if (url == null) "Некорректная или неподдерживаемая ссылка." else try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        null
    } catch (_: android.content.ActivityNotFoundException) {
        "Не найдено приложение для открытия ссылки."
    } catch (_: SecurityException) {
        "Не удалось открыть ссылку: доступ запрещён."
    } catch (_: IllegalArgumentException) {
        "Не удалось открыть ссылку."
    }
    if (error != null) Toast.makeText(context, error, Toast.LENGTH_LONG).show()
}
