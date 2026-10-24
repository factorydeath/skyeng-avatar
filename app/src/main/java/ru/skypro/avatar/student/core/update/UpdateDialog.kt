package ru.skypro.avatar.student.core.update

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

private class AndroidUpdateBackend(private val context: Context) : UpdateBackend {
    override suspend fun check() = UpdateManager.check()
    override suspend fun download(version: String, onProgress: (Int?) -> Unit) = UpdateManager.download(context, version, onProgress)
    override fun canInstall() = UpdateManager.canInstall(context)
    override fun openSettings() = UpdateManager.openUnknownSourcesSettings(context)
    override fun install(apk: File, version: String): Boolean = try {
        UpdateManager.validateApk(context, apk, version)
        UpdateManager.install(context, apk)
    } catch (_: Exception) { false }
}

@Composable
fun UpdateDialog() {
    val context = LocalContext.current.applicationContext
    val vm: UpdateViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdateViewModel(AndroidUpdateBackend(context)) as T
    })
    val state by vm.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onSettingsReturned()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val version = state.version ?: return
    AlertDialog(
        onDismissRequest = vm::dismiss,
        title = { Text("Доступно обновление") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(
                    visible = state.downloading,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                ) {
                    DownloadProgress(state.progress)
                }
                Text(state.error ?: when {
                    state.downloading -> "Скачивание версии $version…"
                    state.awaitingPermission -> "Разрешите установку и вернитесь в приложение."
                    state.apk != null -> "Версия $version готова к установке."
                    else -> "Вышла версия $version. Скачать и установить?"
                })
            }
        },
        confirmButton = {
            TextButton(onClick = vm::downloadOrInstall, enabled = !state.downloading && !state.awaitingPermission) {
                Text(if (state.apk == null) "Скачать" else "Установить")
            }
        },
        dismissButton = {
            TextButton(onClick = vm::dismiss) { Text(if (state.downloading) "Отменить" else "Позже") }
        },
    )
}

@Composable
private fun DownloadProgress(percent: Int?) {
    val progress by animateFloatAsState(
        targetValue = (percent ?: 0).coerceIn(0, 100) / 100f,
        animationSpec = tween(400),
        label = "updateDownloadProgress",
    )
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        if (percent == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(104.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(104.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
            Text(
                "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}
