package com.antoine.photobookorganizer.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object StorageManager {

    const val FOLDER_INBOX = "Inbox"
    const val FOLDER_TO_LIGHTROOM = "ToLightroom"
    const val FOLDER_EDITED_RETURN = "EditedReturn"
    const val FOLDER_EXPORT = "Export"

    private val ALL_FOLDERS = listOf(FOLDER_INBOX, FOLDER_TO_LIGHTROOM, FOLDER_EDITED_RETURN, FOLDER_EXPORT)

    fun rootDocument(context: Context, treeUriString: String): DocumentFile? {
        val uri = Uri.parse(treeUriString)
        return DocumentFile.fromTreeUri(context, uri)
    }

    /** Creates the standard subfolder structure under the chosen root tree, if missing. */
    fun ensureProjectFolders(context: Context, treeUriString: String): Map<String, DocumentFile> {
        val root = rootDocument(context, treeUriString)
            ?: error("Cannot open project folder - permission may have been revoked.")
        val result = mutableMapOf<String, DocumentFile>()
        for (name in ALL_FOLDERS) {
            val existing = root.findFile(name)
            val dir = if (existing != null && existing.isDirectory) existing
                       else root.createDirectory(name)
                       ?: error("Could not create folder: $name")
            result[name] = dir
        }
        return result
    }

    fun getFolder(context: Context, treeUriString: String, name: String): DocumentFile? {
        val root = rootDocument(context, treeUriString) ?: return null
        return root.findFile(name)
    }

    /** Copies bytes verbatim from a source content Uri into a destination folder, preserving quality (no re-encoding). */
    fun copyInto(context: Context, sourceUri: Uri, destFolder: DocumentFile, displayName: String, mimeType: String): DocumentFile? {
        destFolder.findFile(displayName)?.let { return it }
        val newFile = destFolder.createFile(mimeType, displayName) ?: return null
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
        return newFile
    }

    fun baseName(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        return base.removeSuffix("-Edit").removeSuffix("_edit").removeSuffix(" edited").trim()
    }
}
