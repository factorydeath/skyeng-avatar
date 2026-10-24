package ru.skypro.avatar.student.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.headlineLarge,
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun StatusChip(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
    )
}

const val AppAnimMs = 200
const val SlotDialogAnimMs = 200

@Composable
fun LoadingBlock() {
    Column(
        Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator(modifier = Modifier.size(40.dp)) }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Что-то пошло не так", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall)
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
fun EmptyBlock(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}

internal fun ellipsizeWords(text: String, max: Int): String {
    if (text.length <= max) return text
    val cut = text.take(max)
    val lastSpace = cut.lastIndexOf(' ')
    val base = if (lastSpace > 0) cut.substring(0, lastSpace) else cut
    return base.trimEnd() + "..."
}

fun shortTime(iso: String, timeZone: String = ""): String =
    ru.skypro.avatar.student.core.time.scheduleTime(iso, timeZone)
