package com.antoine.photobookorganizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val uri: String,
    val fileName: String,
    val dateTaken: Long,
    val status: PhotoStatus = PhotoStatus.CANDIDATE,
    val perceptualHash: Long? = null,
    val blurScore: Double? = null,
    val isDuplicateGroup: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)
