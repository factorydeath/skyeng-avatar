package ru.skypro.avatar.student.core.update

import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal fun versionParts(value: String): List<Int>? {
    if (!Regex("[0-9]{1,9}(\\.[0-9]{1,9}){0,3}").matches(value)) return null
    return value.split('.').map { it.toInt() }
}

internal fun isNewerVersion(remote: String, local: String): Boolean {
    val a = versionParts(remote) ?: return false
    val b = versionParts(local) ?: return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val comparison = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
        if (comparison != 0) return comparison > 0
    }
    return false
}

internal const val MAX_APK_BYTES = 100L * 1024 * 1024

internal suspend fun copyUpdate(
    input: InputStream,
    target: File,
    expectedBytes: Long,
    onProgress: (Int?) -> Unit,
) {
    try {
        if (expectedBytes == 0L || expectedBytes > MAX_APK_BYTES) throw IOException("Invalid APK size")
        target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            var done = 0L
            var lastProgress: Int? = null
            onProgress(if (expectedBytes > 0) 0 else null)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                currentCoroutineContext().ensureActive()
                if (count < 0) break
                done += count
                if (done > MAX_APK_BYTES || (expectedBytes > 0 && done > expectedBytes)) throw IOException("APK too large")
                output.write(buffer, 0, count)
                val progress = if (expectedBytes > 0) (done * 100 / expectedBytes).toInt().coerceAtMost(99) else null
                if (progress != lastProgress) { lastProgress = progress; onProgress(progress) }
            }
            if (done == 0L || (expectedBytes > 0 && done != expectedBytes)) throw IOException("Incomplete APK")
        }
    } catch (e: Exception) {
        target.delete()
        throw e
    }
}

internal data class ApkIdentity(val packageName: String, val versionName: String?, val versionCode: Long, val signers: Set<String>)

internal fun validateIdentity(installed: ApkIdentity, candidate: ApkIdentity, expectedVersion: String) {
    require(candidate.packageName == installed.packageName) { "Package mismatch" }
    require(candidate.versionName == expectedVersion && isNewerVersion(expectedVersion, installed.versionName.orEmpty())) { "Version mismatch" }
    require(candidate.versionCode > installed.versionCode) { "Version code must increase" }
    require(installed.signers.isNotEmpty() && candidate.signers == installed.signers) { "Signing certificate mismatch" }
}
