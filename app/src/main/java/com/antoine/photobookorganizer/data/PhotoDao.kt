package com.antoine.photobookorganizer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY dateTaken ASC")
    fun getForProject(projectId: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY dateTaken ASC")
    suspend fun getForProjectOnce(projectId: Long): List<Photo>

    @Query("SELECT fileName FROM photos WHERE projectId = :projectId")
    suspend fun getFileNamesForProject(projectId: Long): List<String>

    @Insert
    suspend fun insert(photo: Photo): Long

    @Insert
    suspend fun insertAll(photos: List<Photo>)

    @Update
    suspend fun update(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)

    @Query("SELECT COUNT(*) FROM photos WHERE projectId = :projectId AND status != 'CANDIDATE'")
    suspend fun countPlacedOrLater(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM photos WHERE projectId = :projectId AND status = 'FINAL'")
    suspend fun countFinal(projectId: Long): Int
}
