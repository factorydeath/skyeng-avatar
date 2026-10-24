package ru.skypro.avatar.student

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.skypro.avatar.student.core.update.*

class UpdateFilesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun comparesNumericVersionsWithoutDowngrade() {
        assertTrue(isNewerVersion("1.0.10", "1.0.9"))
        assertTrue(isNewerVersion("2.0", "1.99.99"))
        assertFalse(isNewerVersion("1.0.4", "1.0.5"))
        assertFalse(isNewerVersion("1.0.4", "1.0.4"))
        assertFalse(isNewerVersion("1.0.4.0", "1.0.4"))
        for (value in listOf("", "<html>", "1.2-beta", "1..3", "999999999999", "-1.2")) {
            assertNull(versionParts(value))
            assertFalse(isNewerVersion(value, "1.0.4"))
        }
    }

    @Test fun validatesPackageVersionCodeAndAllCurrentSigners() {
        val installed = ApkIdentity("app.example", "1.0.4", 5, setOf("signer"))
        val candidate = installed.copy(versionName = "1.0.5", versionCode = 6)
        validateIdentity(installed, candidate, "1.0.5")
        for (invalid in listOf(
            candidate.copy(packageName = "other"), candidate.copy(versionName = "1.0.6"),
            candidate.copy(versionCode = 5), candidate.copy(signers = emptySet()),
            candidate.copy(signers = setOf("other")), candidate.copy(signers = setOf("signer", "other")),
        )) assertTrue(runCatching { validateIdentity(installed, invalid, "1.0.5") }.isFailure)
        assertTrue(runCatching { validateIdentity(installed, installed, "1.0.4") }.isFailure)
    }

    @Test fun completeTransferAndUnknownLengthAreSupported() = runTest {
        for (length in listOf(10L, -1L)) {
            val target = File(folder.root, "update.part")
            val progress = mutableListOf<Int?>()
            copyUpdate(ByteArrayInputStream(ByteArray(10) { it.toByte() }), target, length, progress::add)
            assertArrayEquals(ByteArray(10) { it.toByte() }, target.readBytes())
            if (length < 0) assertEquals(listOf<Int?>(null), progress)
            else assertTrue(progress.all { it!! in 0..99 })
        }
    }

    @Test fun incompleteOversizeAndEmptyTransfersAreRemoved() = runTest {
        for ((bytes, length) in listOf(10 to 20L, 10 to 5L, 0 to -1L, 0 to 0L, 1 to MAX_APK_BYTES + 1)) {
            val target = File(folder.root, "update.part")
            assertTrue(runCatching { copyUpdate(ByteArrayInputStream(ByteArray(bytes)), target, length) {} }.isFailure)
            assertFalse(target.exists())
        }
    }

    @Test fun networkFailureRemovesPartialFile() = runTest {
        val target = File(folder.root, "update.part")
        val input = object : InputStream() {
            var reads = 0
            override fun read(): Int = throw IOException("offline")
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (reads++ == 0) { b[off] = 1; return 1 }
                throw IOException("offline")
            }
        }
        assertTrue(runCatching { copyUpdate(input, target, -1) {} }.exceptionOrNull() is IOException)
        assertFalse(target.exists())
    }

    @Test fun cancellationDuringReadRemovesPartialFile() = runTest {
        val target = File(folder.root, "update.part")
        val job = launch {
            val owner = currentCoroutineContext().job
            val input = object : ByteArrayInputStream(ByteArray(100)) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    owner.cancel()
                    return super.read(b, off, len)
                }
            }
            copyUpdate(input, target, 100) {}
        }
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(target.exists())
    }
}
