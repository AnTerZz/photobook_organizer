package com.antoine.photobookorganizer.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.antoine.photobookorganizer.viewmodel.MainViewModel

@Composable
fun ShareTargetScreen(viewModel: MainViewModel, sharedUris: List<Uri>, onDone: () -> Unit) {
    val projects by viewModel.projects.collectAsState()

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Add ${sharedUris.size} photo(s) to which project?") },
        text = {
            Column {
                if (projects.isEmpty()) {
                    Text("Create a project first, then share again.")
                }
                projects.forEach { project ->
                    TextButton(onClick = {
                        viewModel.importSharedPhotos(project, sharedUris)
                        onDone()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(project.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDone) { Text("Cancel") }
        }
    )
}
