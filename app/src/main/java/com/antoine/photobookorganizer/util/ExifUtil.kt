package com.antoine.photobookorganizer.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

object ExifUtil {
    private val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    /** Returns date-taken in epoch millis, reading EXIF when available, else null. */
    fun readDateTaken(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                dateStr?.let { exifFormat.parse(it)?.time }
            }
        } catch (e: Exception) {
            null
        }
    }
}
