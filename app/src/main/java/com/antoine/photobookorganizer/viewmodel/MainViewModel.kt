package com.antoine.photobookorganizer.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antoine.photobookorganizer.data.AppDatabase
import com.antoine.photobookorganizer.data.Photo
import com.antoine.photobookorganizer.data.PhotoStatus
import com.antoine.photobookorganizer.data.Project
import com.antoine.photobookorganizer.scan.ProjectScanner
import com.antoine.photobookorganizer.storage.StorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val scanner = ProjectScanner(application)

    val projects: StateFlow<List<Project>> = db.projectDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _completion = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val completion: StateFlow<Map<Long, Int>> = _completion

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    init {
        viewModelScope.launch {
            projects.collect { list -> list.forEach { refreshCompletion(it.id) } }
        }
    }

    fun createProject(name: String, treeUri: Uri) {
        viewModelScope.launch {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val project = Project(name = name, rootFolderUri = treeUri.toString())
            val id = db.projectDao().insert(project)
            StorageManager.ensureProjectFolders(getApplication(), treeUri.toString())
            refreshCompletion(id)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { db.projectDao().delete(project) }
    }

    fun photosFor(projectId: Long): Flow<List<Photo>> = db.photoDao().getForProject(projectId)

    fun scanInbox(project: Project) {
        viewModelScope.launch {
            val added = scanner.scanInbox(project)
            _statusMessage.value = if (added > 0) "Added $added photo(s) from Inbox" else "No new photos found in Inbox"
            refreshCompletion(project.id)
        }
    }

    fun scanEditedReturn(project: Project) {
        viewModelScope.launch {
            val matched = scanner.scanEditedReturn(project)
            _statusMessage.value = if (matched > 0) "Matched $matched edited photo(s)" else "No matching edited photos found"
            refreshCompletion(project.id)
        }
    }

    fun exportFinals(project: Project) {
        viewModelScope.launch {
            val count = scanner.exportFinals(project)
            _statusMessage.value = "Exported $count final photo(s) to the Export folder"
        }
    }

    fun setStatus(project: Project, photo: Photo, status: PhotoStatus) {
        viewModelScope.launch {
            if (status == PhotoStatus.NEEDS_EDIT) {
                scanner.sendToLightroom(project, photo)
            } else {
                db.photoDao().update(photo.copy(status = status))
            }
            refreshCompletion(project.id)
        }
    }

    fun importSharedPhotos(project: Project, uris: List<Uri>) {
        viewModelScope.launch {
            val folders = StorageManager.ensureProjectFolders(getApplication(), project.rootFolderUri)
            val inbox = folders[StorageManager.FOLDER_INBOX] ?: return@launch
            for (uri in uris) {
                val name = queryDisplayName(uri) ?: "shared_${System.currentTimeMillis()}.jpg"
                val mime = getApplication<Application>().contentResolver.getType(uri) ?: "image/*"
                StorageManager.copyInto(getApplication(), uri, inbox, name, mime)
            }
            scanInbox(project)
        }
    }

    fun refreshCompletion(projectId: Long) {
        viewModelScope.launch {
            val pct = scanner.completionPercent(projectId)
            _completion.value = _completion.value.toMutableMap().apply { put(projectId, pct) }
        }
    }

    fun clearMessage() { _statusMessage.value = null }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
