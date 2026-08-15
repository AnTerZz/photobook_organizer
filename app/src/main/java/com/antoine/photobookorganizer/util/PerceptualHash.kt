package com.antoine.photobookorganizer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale

object PerceptualHash {

    fun compute(context: Context, uri: Uri): Long? {
        val bitmap = decodeDownscaled(context, uri, 9, 8) ?: return null
        var hash = 0L
        var bit = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = grayValue(bitmap.getPixel(x, y))
                val right = grayValue(bitmap.getPixel(x + 1, y))
                if (left > right) hash = hash or (1L shl bit)
                bit++
            }
        }
        bitmap.recycle()
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun grayValue(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3
    }

    internal fun decodeDownscaled(context: Context, uri: Uri, w: Int, h: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val origWidth = bounds.outWidth
            val origHeight = bounds.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while (origWidth / (sampleSize * 2) >= w && origHeight / (sampleSize * 2) >= h) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            val scaled = sampled.scale(w, h)
            if (scaled !== sampled) sampled.recycle()
            scaled
        } catch (t: Throwable) {
            null
        }
    }
}
