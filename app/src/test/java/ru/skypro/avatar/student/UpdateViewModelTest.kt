package ru.skypro.avatar.student

import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.skypro.avatar.student.core.update.*

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
    @get:Rule val folder = TemporaryFolder()
    private val dispatcher = StandardTestDispatcher()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }

    private inner class Backend : UpdateBackend {
        var downloads = 0
        var checks = 0
        var installations = 0
        var settings = 0
        var permission = false
        var settingsAvailable = true
        var failDownload = false
        var canceled = false
        var installerAvailable = true
        val file = File(folder.root, "update.apk")
        override suspend fun check(): UpdateCheck { checks++; return UpdateCheck.Available("1.0.5") }
        override suspend fun download(version: String, onProgress: (Int?) -> Unit): File {
            downloads++
            try {
                onProgress(null)
                delay(1_000)
                if (failDownload) error("offline")
                file.writeText("test fixture")
                return file
            } catch (e: CancellationException) { canceled = true; throw e }
        }
        override fun canInstall() = permission
        override fun openSettings(): Boolean { settings++; return settingsAvailable }
        override fun install(apk: File, version: String): Boolean { installations++; return installerAvailable }
    }

    @Test fun doubleTapDownloadsOnceAndWaitsForPermissionReturn() = runTest(dispatcher) {
        val backend = Backend()
        val vm = UpdateViewModel(backend)
        runCurrent()
        vm.downloadOrInstall(); vm.downloadOrInstall()
        assertTrue(vm.state.value.downloading)
        advanceUntilIdle()
        assertEquals(1, backend.downloads)
        assertEquals(1, backend.settings)
        assertEquals(0, backend.installations)
        assertTrue(vm.state.value.awaitingPermission)
        backend.permission = true
        vm.onSettingsReturned(); vm.onSettingsReturned()
        assertEquals(1, backend.installations)
        assertNull(vm.state.value.version)
        assertTrue(backend.file.exists())
    }

    @Test fun denialRetainsApkForRetryWithoutDownload() = runTest(dispatcher) {
        val backend = Backend()
        val vm = UpdateViewModel(backend)
        runCurrent(); vm.downloadOrInstall(); advanceUntilIdle()
        vm.onSettingsReturned()
        assertNotNull(vm.state.value.error)
        assertEquals(backend.file, vm.state.value.apk)
        assertEquals(0, backend.installations)
        backend.permission = true
        vm.downloadOrInstall()
        assertEquals(1, backend.downloads)
        assertEquals(1, backend.installations)
    }

    @Test fun dismissCancelsTransferAndNeverInstalls() = runTest(dispatcher) {
        val backend = Backend()
        val vm = UpdateViewModel(backend)
        runCurrent(); vm.downloadOrInstall(); runCurrent()
        vm.dismiss(); advanceUntilIdle()
        assertTrue(backend.canceled)
        assertNull(vm.state.value.version)
        assertEquals(0, backend.installations)
    }

    @Test fun failedDownloadCanBeRetried() = runTest(dispatcher) {
        val backend = Backend().apply { failDownload = true; permission = true }
        val vm = UpdateViewModel(backend)
        runCurrent(); vm.downloadOrInstall(); advanceUntilIdle()
        assertFalse(vm.state.value.downloading)
        assertNotNull(vm.state.value.error)
        backend.failDownload = false
        vm.downloadOrInstall(); advanceUntilIdle()
        assertEquals(2, backend.downloads)
        assertEquals(1, backend.installations)
    }

    @Test fun missingSettingsDoesNotLaunchInstaller() = runTest(dispatcher) {
        val backend = Backend().apply { settingsAvailable = false }
        val vm = UpdateViewModel(backend)
        runCurrent(); vm.downloadOrInstall(); advanceUntilIdle()
        assertFalse(vm.state.value.awaitingPermission)
        assertNotNull(vm.state.value.error)
        assertEquals(0, backend.installations)
        vm.dismiss()
        assertFalse(backend.file.exists())
    }

    @Test fun failedInstallationAllowsFreshDownload() = runTest(dispatcher) {
        val backend = Backend().apply { permission = true; installerAvailable = false }
        val vm = UpdateViewModel(backend)
        runCurrent(); vm.downloadOrInstall(); advanceUntilIdle()
        assertNull(vm.state.value.apk)
        assertNotNull(vm.state.value.error)
        assertFalse(backend.file.exists())
        backend.installerAvailable = true
        vm.downloadOrInstall(); advanceUntilIdle()
        assertEquals(2, backend.downloads)
        assertNull(vm.state.value.version)
    }
}
