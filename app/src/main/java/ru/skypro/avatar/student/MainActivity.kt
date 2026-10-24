package ru.skypro.avatar.student

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.skypro.avatar.student.BuildConfig
import ru.skypro.avatar.student.core.auth.AuthState
import ru.skypro.avatar.student.core.auth.AuthViewModel
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.update.UpdateDialog
import ru.skypro.avatar.student.ui.components.AppAnimMs
import ru.skypro.avatar.student.ui.components.LoadingBlock
import ru.skypro.avatar.student.ui.navigation.AppNav
import ru.skypro.avatar.student.ui.screens.LoginScreen
import ru.skypro.avatar.student.ui.theme.AppTheme
import ru.skypro.avatar.student.ui.theme.AvatarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            AppLog.printer = { level, tag, msg -> android.util.Log.println(level, tag, msg) }
        } else {
            AppLog.printer = { _, _, _ -> }
        }
        AppLog.i(AppLog.UI, "MainActivity.onCreate")
        setContent {
            val prefs = getSharedPreferences("avatar_misc", MODE_PRIVATE)
            var theme by remember {
                mutableStateOf(
                    prefs.getString("theme_mode", null)
                        ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                        ?: AppTheme.DARK,
                )
            }
            var showDisclaimer by remember {
                mutableStateOf(
                    getSharedPreferences("avatar_misc", MODE_PRIVATE)
                        .getBoolean("disclaimer_shown", false).not(),
                )
            }
            AvatarTheme(mode = theme) {
                UpdateDialog()
                if (showDisclaimer) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Важно") },
                        text = {
                            Text(
                                "Это неофициальная сборка: она никак не согласована с разработчиками платформы. " +
                                    "Приложение поддерживается одним человеком — @psyhotolic.",
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                getSharedPreferences("avatar_misc", MODE_PRIVATE).edit()
                                    .putBoolean("disclaimer_shown", true).apply()
                                showDisclaimer = false
                            }) { Text("Окей") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                getSharedPreferences("avatar_misc", MODE_PRIVATE).edit()
                                    .putBoolean("disclaimer_shown", true).apply()
                                showDisclaimer = false
                                try {
                                    startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://t.me/powerrlabs"),
                                        ),
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    AppLog.e(AppLog.UI, "channel open failed", e)
                                    Toast.makeText(
                                        applicationContext,
                                        "Не найдено приложение для открытия ссылки.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }) { Text("Перейти в канал") }
                        },
                    )
                }
                val vm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(applicationContext) as T
                    }
                })
                androidx.compose.runtime.LaunchedEffect(Unit) { vm.restore() }
                val state by vm.state.collectAsState()
                val account by vm.account.collectAsState()
                val notice by vm.notice.collectAsState()
                androidx.compose.runtime.LaunchedEffect(state) {
                    AppLog.i(AppLog.UI, "authGate state=$state")
                }
                AnimatedContent(
                    targetState = state == AuthState.LoggedIn,
                    transitionSpec = {
                        (slideInHorizontally(tween(AppAnimMs, easing = LinearOutSlowInEasing)) { it / 4 } +
                            fadeIn(tween(AppAnimMs, easing = LinearOutSlowInEasing)))
                            .togetherWith(
                                slideOutHorizontally(tween(AppAnimMs, easing = LinearOutSlowInEasing)) { -it / 4 } +
                                    fadeOut(tween(AppAnimMs, easing = LinearOutSlowInEasing)),
                            )
                    },
                    label = "authGate",
                ) { loggedIn ->
                    val session = account
                    if (loggedIn && session != null) {
                        key(session) {
                            CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                                AppNav(
                                    client = session.client,
                                    theme = theme,
                                    onTheme = {
                                        theme = it
                                        prefs.edit().putString("theme_mode", it.name).apply()
                                    },
                                    onLogout = { vm.logout() },
                                )
                            }
                        }
                    } else if (state == AuthState.Restoring) {
                        LoadingBlock()
                    } else {
                        LoginScreen(state, notice = notice) { email, pass -> vm.login(email, pass) }
                    }
                }
            }
        }
    }
}
