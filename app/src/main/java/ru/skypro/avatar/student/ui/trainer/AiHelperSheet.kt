package ru.skypro.avatar.student.ui.trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.skypro.avatar.student.data.aihelper.AiChatMessage
import ru.skypro.avatar.student.data.aihelper.parseAiMessage
import ru.skypro.avatar.student.ui.components.ErrorBlock
import ru.skypro.avatar.student.ui.components.FormulaRichText
import ru.skypro.avatar.student.ui.components.LoadingBlock
import ru.skypro.avatar.student.ui.state.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiHelperSheet(
    taskId: String,
    state: UiState<AiHelperContent>,
    onDismiss: () -> Unit,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
    onLike: (String?, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        BoxWithConstraints(Modifier.fillMaxWidth().imePadding()) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 16.dp, vertical = 8.dp)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ИИ-помощник", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onReset, enabled = state is UiState.Content && !state.data.sending && !state.data.reconnectRequired) {
                        Text("Новый диалог")
                    }
                }
                when (state) {
                    UiState.Loading -> LoadingBlock()
                    is UiState.Error -> ErrorBlock(state.message, onRetry)
                    is UiState.Content -> AiChatBody(state.data, onInput, onSend, onLike, onRetry)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AiChatBody(
    content: AiHelperContent,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onLike: (String?, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(content.messages.size) {
        if (content.messages.isNotEmpty()) listState.animateScrollToItem(content.messages.size - 1)
    }
    if (content.messages.isEmpty()) {
        Text(
            "Спроси про эту задачу — помощник подскажет ход решения.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    } else {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = 8.dp),
        ) {
            items(content.messages, key = { it.localId }) { message ->
                AiMessageRow(message, onLike)
            }
        }
    }
    if (content.sending) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
    if (content.reconnectRequired) TextButton(onClick = onRetry) { Text("Переподключиться и загрузить историю") }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = content.input,
            onValueChange = onInput,
            label = { Text("Вопрос") },
            singleLine = false,
            maxLines = 4,
            enabled = !content.sending && !content.reconnectRequired,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSend, enabled = !content.sending && !content.reconnectRequired && content.input.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
        }
    }
}

@Composable
private fun AiMessageRow(message: AiChatMessage, onLike: (String?, Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start,
    ) {
        Card(
            colors = if (message.isFromUser) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            },
        ) {
            val parts = remember(message.text) { parseAiMessage(message.text) }
            Column(Modifier.padding(12.dp)) {
                FormulaRichText(parts)
            }
        }
        if (!message.isFromUser && message.itemId != null) {
            Row {
                IconButton(onClick = { onLike(message.itemId, true) }) {
                    Icon(
                        if (message.like == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Полезно",
                    )
                }
                IconButton(onClick = { onLike(message.itemId, false) }) {
                    Icon(
                        if (message.like == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Не полезно",
                    )
                }
            }
        }
    }
}
