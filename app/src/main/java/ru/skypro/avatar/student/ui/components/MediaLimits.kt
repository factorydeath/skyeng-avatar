package ru.skypro.avatar.student.ui.components

object MediaLimits {
    const val IMAGE_BYTES = 8 * 1024 * 1024
    const val IMAGE_PIXELS = 4_000_000L
    const val FORMULA_CHARACTERS = 4096
    const val FORMULA_PIXELS = 2_000_000L

    fun fits(width: Int, height: Int, pixels: Long): Boolean =
        width > 0 && height > 0 && width.toLong() * height <= pixels

    fun imageSampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 0
        var sample = 1
        while (!fits(((width.toLong() + sample - 1) / sample).toInt(), ((height.toLong() + sample - 1) / sample).toInt(), IMAGE_PIXELS)) {
            if (sample > Int.MAX_VALUE / 2) return 0
            sample *= 2
        }
        return sample
    }
}
