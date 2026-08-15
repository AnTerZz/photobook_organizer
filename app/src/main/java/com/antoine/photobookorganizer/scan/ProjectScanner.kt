package com.antoine.photobookorganizer.scan

import android.content.Context
import android.net.Uri
import com.antoine.photobookorganizer.data.AppDatabase
import com.antoine.photobookorganizer.data.Photo
import com.antoine.photobookorganizer.data.PhotoStatus
import com.antoine.photobookorganizer.data.Project
import com.antoine.photobookorganizer.storage.StorageManager
import com.antoine.photobookorganizer.util.BlurDetector
import com.antoine.photobookorganizer.util.ExifUtil
import com.antoine.photobookorganizer.util.PerceptualHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ProjectScanner(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val imageExtensions = setOf("jpg", "jpeg", "png", "heic", "heif", "dng")
    private val scanMutex = Mutex()

    suspend fun scanInbox(project: Project): Int = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            dedupeExactFileReferences(project.id)

            val folders = StorageManager.ensureProjectFolders(context, project.rootFolderUri)
            val inbox = folders[StorageManager.FOLDER_INBOX] ?: return@withLock 0
            val known = db.photoDao().getFileNamesForProject(project.id).toSet()

            var added = 0
            val newPhotos = mutableListOf<Photo>()
            for (file in inbox.listFiles()) {
                if (!file.isFile) continue
                val name = file.name ?: continue
                if (name in known) continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in imageExtensions) continue

                val dateTaken = ExifUtil.readDateTaken(context, file.uri)
                    ?: file.lastModified().takeIf { it > 0 }
                    ?: System.currentTimeMillis()
                val hash = PerceptualHash.compute(context, file.uri)
                val blur = BlurDetector.computeBlurScore(context, file.uri)

                newPhotos.add(
                    Photo(
                        projectId = project.id,
                        uri = file.uri.toString(),
                        fileName = name,
                        dateTaken = dateTaken,
                        status = PhotoStatus.CANDIDATE,
                        perceptualHash = hash,
                        blurScore = blur
                    )
                )
                added++
            }
            if (newPhotos.isNotEmpty()) {
                db.photoDao().insertAll(newPhotos)
                flagDuplicates(project.id)
            }
            added
        }
    }

    private suspend fun dedupeExactFileReferences(projectId: Long) {
        val photos = db.photoDao().getForProjectOnce(projectId)
        val byUri = photos.groupBy { it.uri }
        for (group in byUri.values) {
            if (group.size > 1) {
                val keepId = group.minOf { it.id }
                for (p in group) {
                    if (p.id != keepId) {
                        db.photoDao().delete(p)
                    }
                }
            }
        }
    }

    private suspend fun flagDuplicates(projectId: Long, threshold: Int = 6) {
        val photos = db.photoDao().getForProjectOnce(projectId).filter { it.perceptualHash != null }
        var nextGroupId = (photos.mapNotNull { it.isDuplicateGroup }.maxOrNull() ?: 0L) + 1
        val visited = mutableSetOf<Long>()

        for (i in photos.indices) {
            val a = photos[i]
            if (a.id in visited) continue
            val group = mutableListOf(a)
            for (j in i + 1 until photos.size) {
                val b = photos[j]
                if (b.id in visited) continue
                val dist = PerceptualHash.hammingDistance(a.perceptualHash!!, b.perceptualHash!!)
                if (dist <= threshold) group.add(b)
            }
            if (group.size > 1) {
                val groupId = a.isDuplicateGroup ?: nextGroupId++
                for (p in group) {
                    visited.add(p.id)
                    if (p.isDuplicateGroup != groupId) {
                        db.photoDao().update(p.copy(isDuplicateGroup = groupId))
                    }
                }
            }
        }
    }

    suspend fun sendToLightroom(project: Project, photo: Photo) = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            val folders = StorageManager.ensureProjectFolders(context, project.rootFolderUri)
            val dest = folders[StorageManager.FOLDER_TO_LIGHTROOM] ?: return@withLock
            val srcUri = Uri.parse(photo.uri)
            val mime = context.contentResolver.getType(srcUri) ?: "image/*"
            StorageManager.copyInto(context, srcUri, dest, photo.fileName, mime)
            db.photoDao().update(photo.copy(status = PhotoStatus.NEEDS_EDIT))
        }
    }

    suspend fun scanEditedReturn(project: Project): Int = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            val folders = StorageManager.ensureProjectFolders(context, project.rootFolderUri)
            val returnFolder = folders[StorageManager.FOLDER_EDITED_RETURN] ?: return@withLock 0
            val pending = db.photoDao().getForProjectOnce(project.id).filter { it.status == PhotoStatus.NEEDS_EDIT }
            if (pending.isEmpty()) return@withLock 0

            var matched = 0
            for (file in returnFolder.listFiles()) {
                if (!file.isFile) continue
                val name = file.name ?: continue
                val base = StorageManager.baseName(name)
                val hit = pending.firstOrNull { StorageManager.baseName(it.fileName) == base && it.status == PhotoStatus.NEEDS_EDIT }
                if (hit != null) {
                    db.photoDao().update(
                        hit.copy(
                            uri = file.uri.toString(),
                            fileName = name,
                            status = PhotoStatus.FINAL
                        )
                    )
                    matched++
                }
            }
            matched
        }
    }

    suspend fun exportFinals(project: Project): Int = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            val folders = StorageManager.ensureProjectFolders(context, project.rootFolderUri)
            val exportFolder = folders[StorageManager.FOLDER_EXPORT] ?: return@withLock 0
            val finals = db.photoDao().getForProjectOnce(project.id).filter { it.status == PhotoStatus.FINAL }
            var count = 0
            for (photo in finals) {
                val srcUri = Uri.parse(photo.uri)
                val mime = context.contentResolver.getType(srcUri) ?: "image/*"
                val result = StorageManager.copyInto(context, srcUri, exportFolder, photo.fileName, mime)
                if (result != null) count++
            }
            count
        }
    }

    suspend fun deletePhoto(photo: Photo) = withContext(Dispatchers.IO) {
        StorageManager.deleteFile(context, photo.uri)
        db.photoDao().delete(photo)
    }

    suspend fun completionPercent(projectId: Long): Int {
        val selectedOrLater = db.photoDao().countSelectedOrLater(projectId)
        if (selectedOrLater == 0) return 0
        val final = db.photoDao().countFinal(projectId)
        return ((final.toDouble() / selectedOrLater) * 100).toInt()
    }
}
