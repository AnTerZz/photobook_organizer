package com.antoine.photobookorganizer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antoine.photobookorganizer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(viewModel: MainViewModel, onOpenProject: (Long) -> Unit) {
    val projects by viewModel.projects.collectAsState()
    val completion by viewModel.completion.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf("") }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && pendingName.isNotBlank()) {
            viewModel.createProject(pendingName, uri)
        }
        pendingName = ""
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Photobook Projects") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New project")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(projects, key = { it.id }) { project ->
                val pct = completion[project.id] ?: 0
                ListItem(
                    headlineContent = { Text(project.name) },
                    supportingContent = {
                        Column {
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                            Text("$pct% complete", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.clickable { onOpenProject(project.id) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New photobook project") },
            text = {
                OutlinedTextField(
                    value = pendingName,
                    onValueChange = { pendingName = it },
                    label = { Text("Project name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    if (pendingName.isNotBlank()) pickFolder.launch(null)
                }) { Text("Choose folder") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
