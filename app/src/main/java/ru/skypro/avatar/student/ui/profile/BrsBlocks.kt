package ru.skypro.avatar.student.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.skypro.avatar.student.domain.day.BrsSubject
import ru.skypro.avatar.student.domain.day.BrsSummary
import ru.skypro.avatar.student.domain.day.BrsWeek
import ru.skypro.avatar.student.domain.day.formatPlanned
import ru.skypro.avatar.student.domain.day.formatScore
import ru.skypro.avatar.student.domain.day.percent
import ru.skypro.avatar.student.domain.day.brsChartMaximum
import ru.skypro.avatar.student.domain.day.brsChartFraction
import ru.skypro.avatar.student.ui.components.EmptyBlock
import ru.skypro.avatar.student.ui.components.ErrorBlock
import ru.skypro.avatar.student.ui.components.LoadingBlock
import ru.skypro.avatar.student.ui.state.UiState

const val BRS_SUBJECTS_PAGE_SIZE = 5

fun <T> List<T>.pageItems(page: Int, size: Int): List<T> {
    if (isEmpty() || page < 0 || size <= 0) return emptyList()
    val from = page.toLong() * size
    if (from >= this.size) return emptyList()
    return subList(from.toInt(), minOf(from + size, this.size.toLong()).toInt())
}

fun pageCount(total: Int, size: Int): Int = if (total <= 0 || size <= 0) 0 else ((total.toLong() + size - 1) / size).toInt()

@Composable
fun BrsBlocks(state: UiState<BrsSummary?>, onRetry: () -> Unit) {
    when (state) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBlock(state.message, onRetry)
        is UiState.Content -> {
            val data = state.data
            if (data == null) EmptyBlock("Баллы пока недоступны")
            else Column {
                BrsSummaryCard(data)
                BrsWeeksCard(data.weeks)
                BrsSubjectsCard(data.subjects)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BrsSummaryCard(data: BrsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Мои баллы", style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatScore(data.totalEarned)} / ${formatScore(data.totalAvailable)}",
                style = MaterialTheme.typography.headlineMedium,
            )
            val pct = data.percent.coerceIn(0, 100)
            val progress by animateFloatAsState(
                targetValue = pct / 100f,
                animationSpec = tween(600),
                label = "brsTotalProgress",
            )
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    Text(
                        "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.offset {
                            with(density) {
                                val halfLabel = 20.dp.toPx()
                                val x = (maxWidth.toPx() * progress - halfLabel)
                                    .coerceIn(0f, (maxWidth.toPx() - halfLabel * 2).coerceAtLeast(0f))
                                IntOffset(x.roundToInt(), 0)
                            }
                        },
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                )
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrsStatColumn("Академические", "${formatScore(data.academicEarned)} / ${formatPlanned(data.academicPlanned)}")
                BrsStatColumn("Дисциплинарные", "${data.disciplinaryCurrent} / ${data.disciplinaryInitial}")
                BrsStatColumn("Баллы вклада", "+${data.contribution}")
            }
        }
    }
}

@Composable
private fun BrsStatColumn(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BrsWeeksCard(weeks: List<BrsWeek>) {
    if (weeks.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Баллы по неделям", style = MaterialTheme.typography.titleMedium)
            Text("Получено / доступно", style = MaterialTheme.typography.bodySmall)
            val max = brsChartMaximum(weeks)
            val barColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                weeks.forEach { week ->
                    Column(
                        Modifier.width(72.dp).clearAndSetSemantics {
                            contentDescription = "Неделя ${week.week}: получено ${formatScore(week.earned)}, доступно ${formatScore(week.available)} баллов"
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("${formatScore(week.earned)} / ${formatScore(week.available)}", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Canvas(Modifier.height(100.dp).fillMaxWidth().padding(horizontal = 6.dp)) {
                            val barWidth = size.width
                            val availableH = size.height * brsChartFraction(week.available, max)
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(0f, size.height - availableH),
                                size = Size(barWidth, availableH),
                                cornerRadius = CornerRadius(8f, 8f),
                            )
                            val earnedH = size.height * brsChartFraction(week.earned, max)
                            if (earnedH > 0) {
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(0f, size.height - earnedH),
                                    size = Size(barWidth, earnedH),
                                    cornerRadius = CornerRadius(8f, 8f),
                                )
                            }
                        }
                        Text("${week.week}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrsSubjectsCard(subjects: List<BrsSubject>) {
    if (subjects.isEmpty()) return
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = pageCount(subjects.size, BRS_SUBJECTS_PAGE_SIZE)
    val safePage = page.coerceIn(0, (pages - 1).coerceAtLeast(0))
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Предметы", style = MaterialTheme.typography.titleMedium)
            subjects.pageItems(safePage, BRS_SUBJECTS_PAGE_SIZE).forEach { subject ->
                BrsSubjectRow(subject)
            }
            if (pages > 1) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { page = (safePage - 1).coerceAtLeast(0) }, enabled = safePage > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    Text("${safePage + 1} / $pages", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { page = (safePage + 1).coerceAtMost(pages - 1) }, enabled = safePage < pages - 1) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Вперёд")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrsSubjectRow(subject: BrsSubject) {
    val pct = subject.percent()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text(subject.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${formatScore(subject.earned)} / ${formatScore(subject.available)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { pct.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text(
                "$pct%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.End)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
