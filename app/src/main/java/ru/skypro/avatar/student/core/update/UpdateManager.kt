package ru.skypro.avatar.student.core.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import ru.skypro.avatar.student.core.network.ApiException
import ru.skypro.avatar.student.core.network.retryApi
import ru.skypro.avatar.student.BuildConfig
import ru.skypro.avatar.student.core.logging.AppLog

sealed interface UpdateCheck {
    data object UpToDate : UpdateCheck
    data class Available(val remote: String) : UpdateCheck
    data class Failed(val reason: String) : UpdateCheck
}

object UpdateManager {
    fun currentVersion(): String = BuildConfig.VERSION_NAME

    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        try {
            val remote = retryApi(AppLog.UI, "update version") {
                val conn = openConnection(BuildConfig.UPDATE_VERSION_URL)
                try {
                    conn.inputStream.use { input ->
                        val bytes = ByteArray(257)
                        var size = 0
                        while (size < bytes.size) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(bytes, size, bytes.size - size)
                            if (count < 0) break
                            size += count
                        }
                        require(size <= 256) { "Version response too large" }
                        bytes.copyOf(size).toString(Charsets.UTF_8).trim()
                    }
                } finally { conn.disconnect() }
            }
            currentCoroutineContext().ensureActive()
            require(versionParts(remote) != null) { "Invalid version" }
            if (isNewerVersion(remote, currentVersion())) UpdateCheck.Available(remote)
            else UpdateCheck.UpToDate
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(AppLog.UI, "update check failed", e)
            UpdateCheck.Failed("Не удалось проверить обновления.")
        }
    }

    suspend fun download(context: Context, expectedVersion: String, onProgress: (Int?) -> Unit): File {
        var completed: File? = null
        try {
            return withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "updates")
                check(dir.isDirectory || dir.mkdirs()) { "Cannot create update directory" }
                val partial = File.createTempFile("avatar-", ".part", dir)
                val ready = File(dir, "${partial.nameWithoutExtension}.apk")
                try {
                    retryApi(AppLog.UI, "update download") {
                        val conn = openConnection(BuildConfig.UPDATE_APK_URL)
                        try {
                            conn.inputStream.use { copyUpdate(it, partial, conn.contentLengthLong, onProgress) }
                        } finally { conn.disconnect() }
                    }
                    currentCoroutineContext().ensureActive()
                    validateApk(context, partial, expectedVersion)
                    currentCoroutineContext().ensureActive()
                    check(partial.renameTo(ready)) { "Cannot finish update download" }
                    completed = ready
                    ready
                } catch (e: Exception) {
                    ready.delete()
                    throw e
                } finally {
                    partial.delete()
                    if (!currentCoroutineContext().isActive) ready.delete()
                }
            }
        } catch (e: Exception) {
            completed?.delete()
            throw e
        }
    }

    private const val TIMEOUT_MS = 15_000

    private suspend fun openConnection(address: String): HttpURLConnection {
        var url = URL(address)
        repeat(6) {
            require(url.protocol == "https") { "Update requires HTTPS" }
            currentCoroutineContext().ensureActive()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                val code = conn.responseCode
                currentCoroutineContext().ensureActive()
                if (code in listOf(301, 302, 303, 307, 308)) {
                    url = URL(url, conn.getHeaderField("Location") ?: throw IOException("Missing redirect"))
                    conn.disconnect()
                } else {
                    if (code != 200) throw ApiException.Http(code)
                    return conn
                }
            } catch (e: Exception) { conn.disconnect(); throw e }
        }
        throw IOException("Too many redirects")
    }

    @Suppress("DEPRECATION")
    internal fun validateApk(context: Context, apk: File, expectedVersion: String) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = pm.getPackageInfo(context.packageName, flags)
        val candidate = pm.getPackageArchiveInfo(apk.absolutePath, flags) ?: error("Invalid APK")
        validateIdentity(installed.identity(), candidate.identity(), expectedVersion)
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.identity(): ApkIdentity {
        val certificates = if (Build.VERSION.SDK_INT >= 28) signingInfo?.apkContentsSigners else signatures
        return ApkIdentity(
            packageName, versionName,
            if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong(),
            certificates.orEmpty().map { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
            }.toSet(),
        )
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(context: Context): Boolean =
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: ActivityNotFoundException) {
            AppLog.e(AppLog.UI, "unknown sources settings not found", e)
            false
        } catch (e: SecurityException) {
            AppLog.e(AppLog.UI, "unknown sources settings denied", e)
            false
        }

    fun install(context: Context, apk: File): Boolean =
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", apk,
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
            true
        } catch (e: ActivityNotFoundException) {
            AppLog.e(AppLog.UI, "install intent: no installer", e)
            false
        } catch (e: IllegalArgumentException) {
            AppLog.e(AppLog.UI, "install intent: uri failed", e)
            false
        } catch (e: SecurityException) {
            AppLog.e(AppLog.UI, "install intent: denied", e)
            false
        }
}
