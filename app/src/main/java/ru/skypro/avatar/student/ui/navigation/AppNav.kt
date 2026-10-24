package ru.skypro.avatar.student.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import ru.skypro.avatar.student.ui.components.AppAnimMs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.skypro.avatar.student.core.logging.AppLog
import io.ktor.client.HttpClient
import ru.skypro.avatar.student.data.day.KtorDayExtrasRepository
import ru.skypro.avatar.student.data.docs.KtorDocumentsRepository
import ru.skypro.avatar.student.data.schedule.KtorScheduleRepository
import ru.skypro.avatar.student.data.subjects.KtorSubjectsRepository
import ru.skypro.avatar.student.data.profile.KtorProfileRepository
import ru.skypro.avatar.student.ui.docs.DocumentsViewModel
import ru.skypro.avatar.student.ui.myday.MyDayViewModel
import ru.skypro.avatar.student.ui.profile.ProfileViewModel
import ru.skypro.avatar.student.ui.schedule.ScheduleViewModel
import ru.skypro.avatar.student.ui.state.UiState
import ru.skypro.avatar.student.ui.subjects.SubjectsViewModel
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import ru.skypro.avatar.student.ui.screens.DocumentsScreen
import ru.skypro.avatar.student.ui.screens.MyDayScreen
import ru.skypro.avatar.student.ui.screens.ProfileScreen
import ru.skypro.avatar.student.ui.screens.ScheduleScreen
import ru.skypro.avatar.student.ui.screens.SubjectDetailScreen
import ru.skypro.avatar.student.ui.screens.SubjectsScreen
import ru.skypro.avatar.student.ui.subjects.SubjectDetailViewModel
import ru.skypro.avatar.student.ui.trainer.TrainerScreen
import ru.skypro.avatar.student.ui.trainer.TrainerViewModel
import ru.skypro.avatar.student.data.aihelper.AiHelperRepository
import ru.skypro.avatar.student.data.trainer.KtorTrainerRepository
import ru.skypro.avatar.student.ui.trainer.AiHelperViewModel
import ru.skypro.avatar.student.ui.theme.AppTheme
import ru.skypro.avatar.student.core.network.openExternal
import ru.skypro.avatar.student.core.time.studyClock
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.runtime.saveable.rememberSaveable

private val tabs = listOf(
    Triple(Routes.MY_DAY, "Мой день", Icons.Filled.Home),
    Triple(Routes.SCHEDULE, "Расписание", Icons.Filled.DateRange),
    Triple(Routes.SUBJECTS, "Предметы", Icons.Filled.School),
    Triple(Routes.DOCUMENTS, "Документы", Icons.Filled.Description),
    Triple(Routes.PROFILE, "Профиль", Icons.Filled.Person),
)

private fun tabDirection(
    from: androidx.navigation.NavBackStackEntry,
    to: androidx.navigation.NavBackStackEntry,
): AnimatedContentTransitionScope.SlideDirection {
    val fromIndex = tabs.indexOfFirst { it.first == from.destination.route }
    val toIndex = tabs.indexOfFirst { it.first == to.destination.route }
    return if (toIndex >= fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}

@Composable
fun AppNav(
    client: HttpClient,
    theme: AppTheme = AppTheme.SYSTEM,
    onTheme: (AppTheme) -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    LaunchedEffect(current) { AppLog.d(AppLog.UI, "nav route=$current") }
    val context = LocalContext.current
    val scheduleVm: ScheduleViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScheduleViewModel(KtorScheduleRepository(client), client.studyClock()) as T
    })
    val subjectsVm: SubjectsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SubjectsViewModel(KtorSubjectsRepository(client)) as T
    })
    val myDayVm: MyDayViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MyDayViewModel(KtorScheduleRepository(client), client.studyClock()) as T
    })
    val weekState by scheduleVm.week.collectAsState()
    val subjectsState by subjectsVm.subjects.collectAsState()
    val myDayState by myDayVm.state.collectAsState()
    val docsVm: DocumentsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DocumentsViewModel(KtorDocumentsRepository(client)) as T
    })
    val docsState by docsVm.docs.collectAsState()
    val profileVm: ProfileViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(KtorProfileRepository(client), KtorDayExtrasRepository(client)) as T
    })
    val profileState by profileVm.summary.collectAsState()
    val brsState by profileVm.brs.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val refresh = {
        scheduleVm.loadCurrentWeek()
        subjectsVm.load()
        myDayVm.load()
        docsVm.load()
        profileVm.load()
    }
    LaunchedEffect(client) { refresh() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(client, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var lastDay = client.studyClock().today()
            while (isActive) {
                delay(30_000)
                val today = client.studyClock().today()
                if (today != lastDay) {
                    lastDay = today
                    scheduleVm.loadCurrentWeek()
                    myDayVm.load()
                }
            }
        }
    }
    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(current) {
        if (previousRoute == Routes.TRAINER && current != null && current != Routes.TRAINER) refresh()
        previousRoute = current
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = { nav.navigate(route) { launchSingleTop = true } },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp)) },
                        label = { Text(label) },
                        alwaysShowLabel = false,
                    )
                }
            }
        },
    ) { pad ->
        NavHost(
            nav,
            startDestination = Routes.MY_DAY,
            modifier = Modifier.padding(pad),
            enterTransition = {
                slideIntoContainer(tabDirection(initialState, targetState), tween(AppAnimMs, easing = LinearOutSlowInEasing)) +
                    fadeIn(tween(AppAnimMs, easing = LinearOutSlowInEasing))
            },
            exitTransition = {
                slideOutOfContainer(tabDirection(initialState, targetState), tween(AppAnimMs, easing = LinearOutSlowInEasing)) +
                    fadeOut(tween(AppAnimMs, easing = LinearOutSlowInEasing))
            },
            popEnterTransition = {
                slideIntoContainer(tabDirection(targetState, initialState), tween(AppAnimMs, easing = LinearOutSlowInEasing)) +
                    fadeIn(tween(AppAnimMs, easing = LinearOutSlowInEasing))
            },
            popExitTransition = {
                slideOutOfContainer(tabDirection(targetState, initialState), tween(AppAnimMs, easing = LinearOutSlowInEasing)) +
                    fadeOut(tween(AppAnimMs, easing = LinearOutSlowInEasing))
            },
        ) {
            composable(Routes.MY_DAY) {
                MyDayScreen(
                    state = myDayState,
                    onRetry = { myDayVm.load() },
                    onOpenUrl = { url -> openExternal(context, url) },
                    onTrainer = { testId -> nav.navigate(Routes.trainer(testId)) },
                )
            }
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    state = weekState,
                    onTrainer = { testId -> nav.navigate(Routes.trainer(testId)) },
                    onRetry = { scheduleVm.loadCurrentWeek() },
                    onOpenUrl = { url -> openExternal(context, url) },
                )
            }
            composable(Routes.SUBJECTS) {
                SubjectsScreen(state = subjectsState, onRetry = { subjectsVm.load() }) { code ->
                    nav.navigate(Routes.subjectDetail(code))
                }
            }
            composable(Routes.SUBJECT_DETAIL) { entry ->
                val code = entry.arguments?.getString("code") ?: ""
                val detailVm: SubjectDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        SubjectDetailViewModel(KtorSubjectsRepository(client)) as T
                })
                val detailState by detailVm.detail.collectAsState()
                LaunchedEffect(code, entry) {
                    entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { detailVm.load(code) }
                }
                SubjectDetailScreen(
                    title = code,
                    state = detailState,
                    onRetry = { detailVm.load(code) },
                    onOpenUrl = { url -> openExternal(context, url) },
                )
            }
            composable(Routes.DOCUMENTS) {
                DocumentsScreen(
                    state = docsState,
                    onRetry = { docsVm.load() },
                    onOpen = { id ->
                        scope.launch {
                            val url = docsVm.openUrl(id)
                            if (url == null) {
                                AppLog.w(AppLog.UI, "docs openUrl null id=$id")
                                android.widget.Toast.makeText(
                                    context,
                                    "Не удалось получить ссылку на документ. Попробуй позже.",
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                openExternal(context, url)
                            }
                        }
                    },
                )
            }
            composable(Routes.TRAINER) { entry ->
                val testId = entry.arguments?.getString("testId") ?: ""
                val trainerRepo = remember { KtorTrainerRepository(client) }
                val aiRepo = remember { AiHelperRepository(client) }
                val trainerVm: TrainerViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        TrainerViewModel(trainerRepo, testId) as T
                })
                val aiVm: AiHelperViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        AiHelperViewModel(aiRepo) as T
                })
                val trainerState by trainerVm.state.collectAsState()
                val helperState by aiVm.state.collectAsState()
                val helperTaskId by aiVm.openTask.collectAsState()
                LaunchedEffect(testId) { trainerVm.start() }
                TrainerScreen(
                    state = trainerState,
                    onBack = { nav.popBackStack() },
                    onAnswerChange = { trainerVm.onAnswerChange(it) },
                    onCheck = { trainerVm.check() },
                    onNext = { trainerVm.next() },
                    onSkip = { trainerVm.requestSkip() },
                    onSkipConfirm = { trainerVm.confirmSkip() },
                    onSkipDismiss = { trainerVm.dismissSkip() },
                    onRetry = { trainerVm.retryStart() },
                    loadImage = { url -> trainerRepo.image(url) },
                    helperTaskId = helperTaskId,
                    helperState = helperState,
                    onOpenHelper = {
                        val taskId = (trainerState as? UiState.Content)?.data?.task?.id
                        if (taskId != null) {
                            aiVm.open(taskId)
                        }
                    },
                    onCloseHelper = {
                        aiVm.close()
                    },
                    onHelperInput = { aiVm.onInput(it) },
                    onSendHelper = { aiVm.send() },
                    onResetHelper = { aiVm.reset() },
                    onLikeHelper = { itemId, like -> aiVm.like(itemId, like) },
                    onRetryHelper = {
                        val taskId = helperTaskId
                        if (taskId != null) aiVm.retry(taskId)
                    },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    state = profileState,
                    brsState = brsState,
                    theme = theme,
                    onTheme = onTheme,
                    onRetry = { profileVm.load() },
                    onRetryBrs = { profileVm.retryBrs() },
                    onOpenUrl = { url -> openExternal(context, url) },
                    onLogout = onLogout,
                )
            }
        }
    }
}
