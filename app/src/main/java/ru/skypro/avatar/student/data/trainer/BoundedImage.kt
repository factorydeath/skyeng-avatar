package ru.skypro.avatar.student.data.trainer

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.ByteArrayOutputStream
import ru.skypro.avatar.student.ui.components.MediaLimits

internal suspend fun readImageBytes(channel: ByteReadChannel, contentLength: Long?): ByteArray? {
    if (contentLength != null && contentLength > MediaLimits.IMAGE_BYTES) return null
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = channel.readAvailable(buffer, 0, minOf(buffer.size, MediaLimits.IMAGE_BYTES - output.size() + 1))
        if (count < 0) break
        if (output.size().toLong() + count > MediaLimits.IMAGE_BYTES) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
