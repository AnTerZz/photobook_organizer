package com.antoine.photobookorganizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rootFolderUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
