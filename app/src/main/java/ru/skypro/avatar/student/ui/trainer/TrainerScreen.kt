package ru.skypro.avatar.student.ui.trainer

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.skypro.avatar.student.data.trainer.paragraphs
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.domain.trainer.ConditionPart
import ru.skypro.avatar.student.domain.trainer.TrainerTask
import ru.skypro.avatar.student.ui.components.ErrorBlock
import ru.skypro.avatar.student.ui.components.LoadingBlock
import ru.skypro.avatar.student.ui.components.FormulaRichText
import ru.skypro.avatar.student.ui.components.MediaLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.key
import ru.skypro.avatar.student.ui.state.UiState

private data class ConditionImageState(val bitmap: ImageBitmap? = null, val loading: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerScreen(
    state: UiState<TrainerContent>,
    onBack: () -> Unit,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onSkipConfirm: () -> Unit,
    onSkipDismiss: () -> Unit,
    onRetry: () -> Unit,
    loadImage: suspend (String) -> ByteArray?,
    helperTaskId: String?,
    helperState: UiState<AiHelperContent>,
    onOpenHelper: () -> Unit,
    onCloseHelper: () -> Unit,
    onHelperInput: (String) -> Unit,
    onSendHelper: () -> Unit,
    onResetHelper: () -> Unit,
    onLikeHelper: (String?, Boolean) -> Unit,
    onRetryHelper: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тренажёр") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenHelper) { Text("ИИ-помощник") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            when (state) {
                UiState.Loading -> LoadingBlock()
                is UiState.Error -> ErrorBlock(state.message, onRetry)
                is UiState.Content -> TrainerBody(state.data, onAnswerChange, onCheck, onNext, onSkip, loadImage)
            }
        }
    }

    val openTaskId = helperTaskId
    if (openTaskId != null) {
        AiHelperSheet(
            taskId = openTaskId,
            state = helperState,
            onDismiss = onCloseHelper,
            onInput = onHelperInput,
            onSend = onSendHelper,
            onReset = onResetHelper,
            onLike = onLikeHelper,
            onRetry = onRetryHelper,
        )
    }

    if (state is UiState.Content && state.data.skipConfirmVisible) {
        AlertDialog(
            onDismissRequest = onSkipDismiss,
            title = { Text("Пропустить задачу?") },
            text = { Text("Задача будет засчитана как пропущенная.") },
            confirmButton = { TextButton(onClick = onSkipConfirm) { Text("Пропустить") } },
            dismissButton = { TextButton(onClick = onSkipDismiss) { Text("Отмена") } },
        )
    }
}

@Composable
private fun TrainerBody(
    content: TrainerContent,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    loadImage: suspend (String) -> ByteArray?,
) {
    if (content.finished) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Задачи закончились", style = MaterialTheme.typography.titleMedium)
        }
        return
    }
    val task = content.task ?: return
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(task.title, style = MaterialTheme.typography.titleLarge)
        ConditionView(task, loadImage)
        OutlinedTextField(
            value = content.answer,
            onValueChange = onAnswerChange,
            label = { Text("Ответ") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            enabled = !content.checking && !content.moving && content.pendingNext == null && (content.submissionId == null || content.result is TrainerResult.Unavailable),
        )
        Button(
            onClick = onCheck,
            enabled = content.answer.isNotBlank() && !content.checking && !content.moving && content.pendingNext == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (content.checking) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(if (content.submissionId != null) "Получить результат" else "Проверить")
            }
        }
        content.result?.let { ResultView(it) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onSkip,
                enabled = !content.checking && !content.moving && content.pendingNext == null && (content.submissionId == null || content.result is TrainerResult.Unavailable),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text("Пропустить")
            }
            Button(
                onClick = onNext,
                enabled = !content.checking && !content.moving && (content.submissionId == null || content.result is TrainerResult.Unavailable),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (content.moving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (content.pendingNext != null) "Продолжить переход" else "Следующее")
                }
            }
        }
    }
}

@Composable
private fun ConditionImage(url: String, loadImage: suspend (String) -> ByteArray?) {
    key(url) { ConditionImageContent(url, loadImage) }
}

@Composable
private fun ConditionImageContent(url: String, loadImage: suspend (String) -> ByteArray?) {
    val image by produceState(ConditionImageState(), url) {
        val bitmap = loadImage(url)?.let { bytes -> withContext(Dispatchers.Default) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val sample = MediaLimits.imageSampleSize(bounds.outWidth, bounds.outHeight)
                if (sample == 0) return@withContext null
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (decoded != null && !MediaLimits.fits(decoded.width, decoded.height, MediaLimits.IMAGE_PIXELS)) {
                    decoded.recycle()
                    null
                } else decoded?.asImageBitmap()
            } catch (e: Exception) {
                AppLog.w(AppLog.TRAINER, "image decode failed", e)
                null
            } catch (_: OutOfMemoryError) {
                null
            }
        } }
        value = ConditionImageState(bitmap, loading = false)
    }
    val loaded = image.bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text(
            if (image.loading) "Загрузка изображения…" else "Изображение недоступно или превышает допустимый размер",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConditionView(task: TrainerTask, loadImage: suspend (String) -> ByteArray?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        task.condition.paragraphs().forEach { paragraph ->
            val single = paragraph.singleOrNull()
            if (single is ConditionPart.Image) {
                ConditionImage(single.url, loadImage)
                return@forEach
            }
            FormulaRichText(paragraph, MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ResultView(result: TrainerResult) {
    when (result) {
        TrainerResult.Correct -> CorrectBadge()
        TrainerResult.Incorrect -> IncorrectBadge()
        is TrainerResult.Unavailable -> Text(
            result.message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun CorrectBadge() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val color = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(24.dp)) { drawCheck(color) }
        Spacer(Modifier.width(8.dp))
        Text("Верно", style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun IncorrectBadge() {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val steps = listOf(-10f, 10f, -8f, 8f, -4f, 4f, 0f)
        for (x in steps) shake.animateTo(x, tween(durationMillis = 45, easing = FastOutSlowInEasing))
    }
    val color = MaterialTheme.colorScheme.error
    Row(
        Modifier
            .graphicsLayer { translationX = shake.value }
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(24.dp)) { drawCross(color) }
        Spacer(Modifier.width(8.dp))
        Text("Неверно", style = MaterialTheme.typography.titleMedium, color = color)
    }
}

private fun DrawScope.drawCheck(color: Color) {
    val stroke = size.minDimension * 0.14f
    val path = Offset(size.width * 0.18f, size.height * 0.52f) to Offset(size.width * 0.42f, size.height * 0.74f)
    val tail = Offset(size.width * 0.84f, size.height * 0.28f)
    drawLine(color, path.first, path.second, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, path.second, tail, strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun DrawScope.drawCross(color: Color) {
    val stroke = size.minDimension * 0.14f
    val inset = size.minDimension * 0.24f
    drawLine(color, Offset(inset, inset), Offset(size.width - inset, size.height - inset), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width - inset, inset), Offset(inset, size.height - inset), strokeWidth = stroke, cap = StrokeCap.Round)
}
