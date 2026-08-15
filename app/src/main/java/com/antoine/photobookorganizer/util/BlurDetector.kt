package com.antoine.photobookorganizer.util

import android.content.Context
import android.net.Uri

object BlurDetector {

    private const val SIZE = 100

    /** Returns a blur score via variance of a Laplacian edge filter on a downscaled grayscale image. Lower = blurrier. Null if it couldn't be computed. */
    fun computeBlurScore(context: Context, uri: Uri): Double? {
        val bitmap = PerceptualHash.decodeDownscaled(context, uri, SIZE, SIZE) ?: return null
        val gray = IntArray(SIZE * SIZE)
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                gray[y * SIZE + x] = (r + g + b) / 3
            }
        }
        bitmap.recycle()

        val laplacian = DoubleArray((SIZE - 2) * (SIZE - 2))
        var idx = 0
        for (y in 1 until SIZE - 1) {
            for (x in 1 until SIZE - 1) {
                val center = gray[y * SIZE + x]
                val up = gray[(y - 1) * SIZE + x]
                val down = gray[(y + 1) * SIZE + x]
                val left = gray[y * SIZE + x - 1]
                val right = gray[y * SIZE + x + 1]
                laplacian[idx++] = (4 * center - up - down - left - right).toDouble()
            }
        }
        val mean = laplacian.average()
        val variance = laplacian.sumOf { (it - mean) * (it - mean) } / laplacian.size
        return variance
    }

    const val BLUR_WARNING_THRESHOLD = 150.0
}
