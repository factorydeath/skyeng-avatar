package ru.skypro.avatar.student.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import ru.skypro.avatar.student.domain.docs.StudentDocument
import ru.skypro.avatar.student.domain.schedule.Slot
import ru.skypro.avatar.student.domain.schedule.SlotStatus
import ru.skypro.avatar.student.domain.schedule.WeekSchedule
import ru.skypro.avatar.student.domain.subjects.DetailSection
import ru.skypro.avatar.student.domain.subjects.Subject
import ru.skypro.avatar.student.domain.day.BrsSummary
import ru.skypro.avatar.student.ui.profile.BrsBlocks
import ru.skypro.avatar.student.ui.profile.ProfileContent
import ru.skypro.avatar.student.ui.components.EmptyBlock
import ru.skypro.avatar.student.ui.components.ErrorBlock
import ru.skypro.avatar.student.ui.components.LoadingBlock
import ru.skypro.avatar.student.ui.components.ScreenTitle
import ru.skypro.avatar.student.ui.components.SectionHeader
import ru.skypro.avatar.student.ui.components.StatusChip
import ru.skypro.avatar.student.ui.components.AppAnimMs
import ru.skypro.avatar.student.ui.components.SlotDialogAnimMs
import ru.skypro.avatar.student.ui.components.ellipsizeWords
import ru.skypro.avatar.student.ui.components.shortTime
import ru.skypro.avatar.student.ui.myday.MyDayContent
import ru.skypro.avatar.student.ui.preview.previewSlots
import ru.skypro.avatar.student.ui.preview.previewSubjects
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.theme.AppTheme
import ru.skypro.avatar.student.ui.theme.subjectColor

private val subjectNames = mapOf(
    "soft_skills" to "Гибкие навыки",
    "physical_education" to "Физкультура",
    "onboarding" to "Онбординг",
    "career_guidance" to "Профориентация",
    "career_guidance_professions" to "Профессии по выбору",
    "russian" to "Русский язык",
    "geography" to "География",
    "physics" to "Физика",
    "chemistry" to "Химия",
    "biology" to "Биология",
    "basics_of_security" to "Основы безопасности",
    "english" to "Английский язык",
    "math" to "Математика",
)

fun subjectTitle(code: String): String = subjectNames[code] ?: code

@Composable
fun SlotCard(slot: Slot, onOpenUrl: (String) -> Unit = {}, onTrainer: (String) -> Unit = {}) {
    val accent = subjectColor(slot.subjectCode)
    var showDetails by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val closeDetails = { closing = true }
    if (showDetails) {
        Dialog(onDismissRequest = { closeDetails() }) {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            LaunchedEffect(closing) {
                if (closing) {
                    visible = false
                    kotlinx.coroutines.delay(SlotDialogAnimMs.toLong())
                    showDetails = false
                    closing = false
                }
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(SlotDialogAnimMs, easing = LinearOutSlowInEasing)) +
                    scaleIn(tween(SlotDialogAnimMs, easing = LinearOutSlowInEasing), initialScale = 0.94f),
                exit = fadeOut(tween(SlotDialogAnimMs, easing = LinearOutSlowInEasing)) +
                    scaleOut(tween(SlotDialogAnimMs, easing = LinearOutSlowInEasing), targetScale = 0.94f),
            ) {
                Column(
                    Modifier.fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                         .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                         .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(6.dp)
                            .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.2f)))),
                    )
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            subjectTitle(slot.subjectCode).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                        )
                        Text(slot.title.ifEmpty { "Занятие" }, style = MaterialTheme.typography.headlineSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeCapsule(shortTime(slot.startsAtIso, slot.timeZone))
                            Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TimeCapsule(shortTime(slot.endsAtIso, slot.timeZone))
                        }
                        if (slot.statusText.isNotEmpty()) {
                            Text(
                                slot.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        slot.actions.forEach { action ->
                            if (action.trainerTestId != null) {
                                Button(
                                    onClick = { onTrainer(action.trainerTestId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0B0B12)),
                                ) { Text("В тренажёр") }
                            } else if (action.isDebt) {
                                OutlinedButton(
                                    onClick = { onOpenUrl(action.url) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                ) { Text("${action.title} (долг)") }
                            } else {
                                Button(
                                    onClick = { onOpenUrl(action.url) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0B0B12)),
                                ) { Text(action.title) }
                            }
                        }
                        TextButton(
                            onClick = { closeDetails() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Закрыть") }
                    }
                }
            }
        }
    }
    Card(
        onClick = { showDetails = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 56.dp)) {
                Text(shortTime(slot.startsAtIso, slot.timeZone), style = MaterialTheme.typography.titleMedium)
                Text(shortTime(slot.endsAtIso, slot.timeZone), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.padding(horizontal = 12.dp).size(width = 3.dp, height = 48.dp)
                    .clip(CircleShape).background(accent.copy(alpha = 0.8f)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(subjectTitle(slot.subjectCode), style = MaterialTheme.typography.labelMedium, color = accent)
                Text(slot.title.ifEmpty { "Занятие" }, style = MaterialTheme.typography.bodyLarge)
                if (slot.status != SlotStatus.DEBTS && slot.statusText.isNotEmpty()) {
                    Text(slot.statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            slot.actions.firstOrNull { !it.isDebt }?.let { action ->
                if (action.trainerTestId != null) {
                    TextButton(onClick = { onTrainer(action.trainerTestId) }) { Text("В тренажёр") }
                } else {
                    TextButton(onClick = { onOpenUrl(action.url) }) { Text(ellipsizeWords(action.title, 14)) }
                }
            }
        }
    }
}

@Composable
private fun TimeCapsule(time: String) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(time, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SubjectCard(subject: Subject, onClick: () -> Unit = {}) {
    val accent = subjectColor(subject.code)
    val progress = if (subject.lessonsTotal > 0) subject.lessonsDone.toFloat() / subject.lessonsTotal else 0f
    val name = subjectTitle(subject.code)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${subject.lessonsDone} из ${subject.lessonsTotal} уроков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (subject.debts > 0) StatusChip("Долги: ${subject.debts}", MaterialTheme.colorScheme.error)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 5.dp,
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(56.dp),
                    color = accent,
                    strokeWidth = 5.dp,
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}

fun greeting(timeZone: String = ""): String {
    val hour = try {
        java.time.LocalTime.now(runCatching { java.time.ZoneId.of(timeZone) }.getOrDefault(java.time.ZoneId.systemDefault())).hour
    } catch (e: Exception) {
        12
    }
    return when (hour) {
        in 5..11 -> "Доброе утро ☀️"
        in 12..17 -> "Хорошего дня 🌤️"
        in 18..22 -> "Добрый вечер 🌙"
        else -> "Привет 🌙"
    }
}

private val dayPhrases = listOf(
    "У тебя всё получится.",
    "Ты молодец, так держать!",
    "Маленькие шаги — большой результат",
    "Сегодня отличный день для учёбы",
    "Ты уже лучше, чем вчера",
    "Вперёд, к новым знаниям!",
    "Спокойно и по порядку — всё успеешь",
)

fun dayPhrase(): String = dayPhraseFor(LocalDate.now())

internal fun dayPhraseFor(date: LocalDate): String {
    if (date.dayOfWeek == java.time.DayOfWeek.SATURDAY) return "Суббота — день для отдыха 🌿"
    return dayPhrases[date.dayOfYear % dayPhrases.size]
}

@Composable
fun MyDayScreen(
    state: UiState<MyDayContent> = UiState.Content(MyDayContent(previewSlots)),
    onRetry: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onTrainer: (String) -> Unit = {},
) {
    when (state) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBlock(state.message, onRetry)
        is UiState.Content -> {
            val data = state.data
            LazyColumn {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(greeting(data.timeZone), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                dayPhraseFor(data.date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            val first = data.slots.minByOrNull { it.startsAtIso }
                            Text(
                                if (data.slots.isEmpty()) "На сегодня занятий нет — отдыхай"
                                else "Сегодня ${data.slots.size} ${plural(data.slots.size, "занятие", "занятия", "занятий")}" +
                                    (first?.let { " · первое в ${shortTime(it.startsAtIso, it.timeZone)}" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (data.slots.isNotEmpty()) {
                    item { SectionHeader("Сегодня") }
                    items(data.slots) { SlotCard(it, onOpenUrl, onTrainer) }
                }
            }
        }
    }
}

private fun plural(n: Int, one: String, few: String, many: String): String = when {
    n % 10 == 1 && n % 100 != 11 -> one
    n % 10 in 2..4 && (n % 100 < 10 || n % 100 >= 20) -> few
    else -> many
}

private val weekDays = mapOf(
    java.time.DayOfWeek.MONDAY to "Пн",
    java.time.DayOfWeek.TUESDAY to "Вт",
    java.time.DayOfWeek.WEDNESDAY to "Ср",
    java.time.DayOfWeek.THURSDAY to "Чт",
    java.time.DayOfWeek.FRIDAY to "Пт",
    java.time.DayOfWeek.SATURDAY to "Сб",
    java.time.DayOfWeek.SUNDAY to "Вс",
)

fun dayLabel(dateIso: String): String = try {
    val d = LocalDate.parse(dateIso.take(10))
    "${weekDays[d.dayOfWeek]} ${d.dayOfMonth}"
} catch (e: Exception) {
    dateIso.take(10)
}

private fun isSaturday(dateIso: String): Boolean = try {
    LocalDate.parse(dateIso.take(10)).dayOfWeek == java.time.DayOfWeek.SATURDAY
} catch (e: Exception) {
    false
}

@Composable
fun WeekendBlock() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🎉", style = MaterialTheme.typography.displayMedium)
            Text("В этот день нет уроков", style = MaterialTheme.typography.titleLarge)
            Text(
                "УРААА! Отдыхай 🎉",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun ScheduleScreen(
    state: UiState<WeekSchedule> = UiState.Loading,
    onRetry: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onTrainer: (String) -> Unit = {},
) {
    when (state) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBlock(state.message, onRetry)
        is UiState.Content -> {
            val days = state.data.days
            if (days.isEmpty()) {
                ScreenTitle("Расписание")
                EmptyBlock("Нет занятий на неделе")
                return
            }
            val zone = runCatching { java.time.ZoneId.of(state.data.timeZone) }.getOrDefault(java.time.ZoneId.systemDefault())
            val today = LocalDate.now(zone).toString()
            var selected by rememberSaveable(days.map { it.date }, today) {
                mutableIntStateOf(days.indexOfFirst { it.date.take(10) == today }.takeIf { it >= 0 } ?: 0)
            }
            LazyColumn {
                item { ScreenTitle("Расписание") }
                item {
                    LazyRow(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                        items(days.size) { i ->
                            FilterChip(
                                selected = selected == i,
                                onClick = { selected = i },
                                label = { Text(dayLabel(days[i].date)) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
                item {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            val dir = if (targetState >= initialState) {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            }
                            (fadeIn(tween(AppAnimMs, easing = LinearOutSlowInEasing)) +
                                slideIntoContainer(dir, tween(AppAnimMs, easing = LinearOutSlowInEasing)))
                                .togetherWith(
                                    fadeOut(tween(AppAnimMs, easing = LinearOutSlowInEasing)) +
                                        slideOutOfContainer(dir, tween(AppAnimMs, easing = LinearOutSlowInEasing)),
                                )
                        },
                        label = "day",
                    ) { day ->
                        val displayedDay = days.getOrNull(day) ?: return@AnimatedContent
                        val slots = displayedDay.slots
                        if (slots.isEmpty()) {
                            if (isSaturday(displayedDay.date)) WeekendBlock()
                            else EmptyBlock("В этот день занятий нет")
                        } else {
                            Column {
                                slots.forEach { SlotCard(it, onOpenUrl, onTrainer) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectsScreen(
    state: UiState<List<Subject>> = UiState.Content(previewSubjects),
    onRetry: () -> Unit = {},
    onSubjectClick: (String) -> Unit = {},
) {
    when (state) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBlock(state.message, onRetry)
        is UiState.Content ->
            if (state.data.isEmpty()) {
                ScreenTitle("Предметы")
                EmptyBlock("Нет предметов")
            } else LazyColumn {
                item { ScreenTitle("Предметы") }
                items(state.data) { SubjectCard(it) { onSubjectClick(it.code) } }
            }
    }
}

@Composable
fun SubjectDetailScreen(
    title: String,
    state: UiState<List<DetailSection>>,
    onRetry: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    when (state) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBlock(state.message, onRetry)
        is UiState.Content -> LazyColumn {
            item { ScreenTitle(subjectTitle(title)) }
            state.data.forEach { section ->
                item { SectionHeader(section.title) }
                items(section.items) { item ->
                    val cardColor = if (item.isDebt) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.isDebt) StatusChip("Долг", MaterialTheme.colorScheme.error)
                                else if (item.status.isNotEmpty()) StatusChip(item.status)
                            }
                            if (item.openUrl.isNotEmpty()) Button(onClick = { onOpenUrl(item.openUrl) }) {
                                Text("Открыть")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentsScreen(
    state: UiState<List<StudentDocument>> = UiState.Content(emptyList()),
    onRetry: () -> Unit = {},
    onOpen: (Long) -> Unit = {},
) {
    when (state) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBlock(state.message, onRetry)
        is UiState.Content ->
            if (state.data.isEmpty()) {
                ScreenTitle("Документы")
                EmptyBlock("Нет документов")
            } else LazyColumn {
                item { ScreenTitle("Документы") }
                items(state.data) { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Справка ${doc.title}", style = MaterialTheme.typography.titleMedium)
                            Text(doc.createdAt.take(10), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { onOpen(doc.id) }) { Text("Посмотреть") }
                        }
                    }
                }
            }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier, alert: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (alert) MaterialTheme.colorScheme.errorContainer else Color.White,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
fun ProfileScreen(
    state: UiState<ProfileContent> = UiState.Loading,
    brsState: UiState<BrsSummary?> = UiState.Loading,
    theme: AppTheme = AppTheme.SYSTEM,
    onTheme: (AppTheme) -> Unit = {},
    onRetry: () -> Unit = {},
    onRetryBrs: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Выйти?") },
            text = { Text("Точно ли вы хотите выйти из личного кабинета?") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Выйти") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Остаться") }
            },
        )
    }
    LazyColumn {
        item { ScreenTitle("Профиль") }
        when (state) {
            UiState.Loading -> item { LoadingBlock() }
            is UiState.Error -> item { ErrorBlock(state.message, onRetry) }
            is UiState.Content -> {
                val s = state.data.summary
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                                ),
                            )
                            .padding(20.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ProfileStat(
                                value = "${s.lessons?.completed ?: 0}/${s.lessons?.total ?: 0}",
                                label = "Уроков",
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStat(
                                value = "${s.subjects}",
                                label = "Предметов",
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStat(
                                value = "${s.debts}",
                                label = "Долги",
                                alert = s.debts > 0,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (state.data.interests.isNotEmpty()) {
                    item { SectionHeader("Интересы") }
                    item {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.data.interests.forEach { interest ->
                                val fav = interest in state.data.favorites
                                StatusChip(
                                    (if (fav) "★ " else "") + interestTitle(interest),
                                    if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                state.data.personalRouteUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                    item { SectionHeader("Полезные ссылки") }
                    item {
                        Card(
                            onClick = { onOpenUrl(url) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(state.data.personalRouteTitle ?: "Персональный маршрут", style = MaterialTheme.typography.titleSmall)
                                Text(url, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item { BrsBlocks(brsState, onRetryBrs) }
        item { SectionHeader("Тема оформления") }
        item {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { mode ->
                    FilterChip(
                        selected = theme == mode,
                        onClick = { onTheme(mode) },
                        label = { Text(themeTitle(mode)) },
                    )
                }
            }
        }
        item {
            Button(
                onClick = { onOpenUrl(ru.skypro.avatar.student.core.network.ApiEndpoints.EXTERNAL_SETTINGS) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) { Text("Настройки") }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            ) { Text("Выйти") }
        }
    }
}

private val interestNames = mapOf(
    "cinema" to "Кино и сериалы",
    "sport" to "Спорт",
    "music" to "Музыка",
    "games" to "Игры",
    "technology" to "Технологии",
    "internet-culture" to "Мемы и интернет",
    "anime" to "Аниме и манга",
    "travel" to "Путешествия",
    "science" to "Наука",
    "psychology" to "Психология",
    "fashion" to "Мода",
    "business" to "Бизнес",
)

private fun interestTitle(code: String): String = interestNames[code] ?: code

fun themeTitle(mode: AppTheme): String = when (mode) {
    AppTheme.LIGHT -> "Светлая"
    AppTheme.DARK -> "Тёмная"
    AppTheme.BLACK -> "Чёрная"
    AppTheme.SYSTEM -> "Как в системе"
}
