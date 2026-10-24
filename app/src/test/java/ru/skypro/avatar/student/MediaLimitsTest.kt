package ru.skypro.avatar.student

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import ru.skypro.avatar.student.data.trainer.readImageBytes
import ru.skypro.avatar.student.ui.components.MediaLimits

class MediaLimitsTest {
    @Test fun chunkedImageIsBoundedWithoutContentLength() = runTest {
        assertNull(readImageBytes(ByteReadChannel(ByteArray(MediaLimits.IMAGE_BYTES + 1)), null))
        assertEquals(MediaLimits.IMAGE_BYTES, readImageBytes(ByteReadChannel(ByteArray(MediaLimits.IMAGE_BYTES)), null)?.size)
        assertNull(readImageBytes(ByteReadChannel(byteArrayOf(1)), MediaLimits.IMAGE_BYTES + 1L))
    }

    @Test fun largeDimensionsAreSampledWithoutIntegerOverflow() {
        val sample = MediaLimits.imageSampleSize(12000, 9000)
        assertEquals(8, sample)
        assertTrue(MediaLimits.fits(12000 / sample, 9000 / sample, MediaLimits.IMAGE_PIXELS))
        assertFalse(MediaLimits.fits(Int.MAX_VALUE, Int.MAX_VALUE, MediaLimits.FORMULA_PIXELS))
        assertTrue(MediaLimits.imageSampleSize(Int.MAX_VALUE, Int.MAX_VALUE) > 1)
        assertEquals(0, MediaLimits.imageSampleSize(-1, 100))
        assertTrue(MediaLimits.fits(2000, 1000, MediaLimits.FORMULA_PIXELS))
        assertFalse(MediaLimits.fits(2001, 1000, MediaLimits.FORMULA_PIXELS))
    }
}
