package com.antoine.photobookorganizer.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: PhotoStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): PhotoStatus = try {
        PhotoStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PhotoStatus.FINAL
    }
}
