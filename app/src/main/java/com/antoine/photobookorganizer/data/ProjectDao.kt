package com.antoine.photobookorganizer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Project>>

    @Insert
    suspend fun insert(project: Project): Long

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?
}
