package com.antoine.photobookorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.antoine.photobookorganizer.data.Photo
import com.antoine.photobookorganizer.data.PhotoStatus
import com.antoine.photobookorganizer.util.BlurDetector
import com.antoine.photobookorganizer.viewmodel.MainViewModel

private enum class FilterOption(val label: String) {
    ALL("All"),
    DUPLICATES("Duplicates"),
    CANDIDATE("Candidates"),
    SELECTED("Selected"),
    NEEDS_EDIT("Needs Edit"),
    FINAL("Final")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(viewModel: MainViewModel, projectId: Long) {
    val projects by viewModel.projects.collectAsState()
    val project = projects.firstOrNull { it.id == projectId } ?: return
    val photos by viewModel.photosFor(projectId).collectAsState(initial = emptyList())
    val message by viewModel.statusMessage.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var filterOption by remember { mutableStateOf(FilterOption.ALL) }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }
    var showTriage by remember { mutableStateOf(false) }

    val filtered = when (filterOption) {
        FilterOption.ALL -> photos
        FilterOption.DUPLICATES -> photos.filter { it.isDuplicateGroup != null }
        FilterOption.CANDIDATE -> photos.filter { it.status == PhotoStatus.CANDIDATE }
        FilterOption.SELECTED -> photos.filter { it.status == PhotoStatus.SELECTED }
        FilterOption.NEEDS_EDIT -> photos.filter { it.status == PhotoStatus.NEEDS_EDIT }
        FilterOption.FINAL -> photos.filter { it.status == PhotoStatus.FINAL }
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
    }

    if (showTriage) {
        TriageScreen(
            candidates = photos.filter { it.status == PhotoStatus.CANDIDATE },
            onSelect = { photo -> viewModel.setStatus(project, photo, PhotoStatus.SELECTED) },
            onClose = { showTriage = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, fontWeight = FontWeight.Bold)
                        Text(
                            if (photos.size == 1) "1 photo" else "${photos.size} photos",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        },
        snackbarHost = {
            message?.let { Snackbar(modifier = Modifier.padding(12.dp)) { Text(it) } }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(onClick = { viewModel.scanInbox(project) }, enabled = !isScanning) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isScanning) "Scanning..." else "Scan Inbox")
                }
                FilledTonalButton(onClick = { showTriage = true }) { Text("Triage") }
                FilledTonalButton(onClick = { viewModel.scanEditedReturn(project) }, enabled = !isScanning) { Text("Scan Returns") }
                FilledTonalButton(onClick = { viewModel.exportFinals(project) }, enabled = !isScanning) { Text("Export") }
            }

            FilterDropdown(
                selected = filterOption,
                onSelect = { filterOption = it },
                counts = mapOf(
                    FilterOption.ALL to photos.size,
                    FilterOption.DUPLICATES to photos.count { it.isDuplicateGroup != null },
                    FilterOption.CANDIDATE to photos.count { it.status == PhotoStatus.CANDIDATE },
                    FilterOption.SELECTED to photos.count { it.status == PhotoStatus.SELECTED },
                    FilterOption.NEEDS_EDIT to photos.count { it.status == PhotoStatus.NEEDS_EDIT },
                    FilterOption.FINAL to photos.count { it.status == PhotoStatus.FINAL }
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No photos here yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { photo ->
                        PhotoThumbnail(photo = photo, onClick = { selectedPhoto = photo })
                    }
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        PhotoActionSheet(
            photo = photo,
            onDismiss = { selectedPhoto = null },
            onSetStatus = { status ->
                viewModel.setStatus(project, photo, status)
                selectedPhoto = null
            },
            onDeleteDuplicate = {
                viewModel.deletePhoto(photo)
                selectedPhoto = null
            }
        )
    }
}


@Composable
private fun FilterDropdown(selected: FilterOption, onSelect: (FilterOption) -> Unit, counts: Map<FilterOption, Int>) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        FilledTonalButton(onClick = { expanded = true }) {
            Text("${selected.label} (${counts[selected] ?: 0})")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FilterOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.label} (${counts[option] ?: 0})") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: Photo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
            if (photo.isDuplicateGroup != null) {
                StatusBadge(text = "DUP", color = Color(0xFFC1663D))
            }
            val blur = photo.blurScore
            if (blur != null && blur < BlurDetector.BLUR_WARNING_THRESHOLD) {
                StatusBadge(text = "BLUR", color = Color(0xFFA13D2B))
            }
        }
        Text(
            text = statusAbbrev(photo.status),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(end = 4.dp)
            .background(color, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

private fun statusAbbrev(status: PhotoStatus): String = when (status) {
    PhotoStatus.CANDIDATE -> "CAND"
    PhotoStatus.SELECTED -> "SEL"
    PhotoStatus.NEEDS_EDIT -> "EDIT"
    PhotoStatus.FINAL -> "FINAL"
}

private fun statusActionLabel(status: PhotoStatus): String = when (status) {
    PhotoStatus.CANDIDATE -> "Move to Candidates"
    PhotoStatus.SELECTED -> "Select for book"
    PhotoStatus.NEEDS_EDIT -> "Send to Lightroom"
    PhotoStatus.FINAL -> "Mark Final"
}

private fun statusActionIcon(status: PhotoStatus): ImageVector = when (status) {
    PhotoStatus.CANDIDATE -> Icons.Filled.Undo
    PhotoStatus.SELECTED -> Icons.Filled.Star
    PhotoStatus.NEEDS_EDIT -> Icons.Filled.Edit
    PhotoStatus.FINAL -> Icons.Filled.CheckCircle
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoActionSheet(
    photo: Photo,
    onDismiss: () -> Unit,
    onSetStatus: (PhotoStatus) -> Unit,
    onDeleteDuplicate: () -> Unit
) {
    var showFullScreen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { showFullScreen = true },
                contentScale = ContentScale.Fit
            )
            Text(
                photo.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoStatus.entries.filter { it != photo.status }.forEach { status ->
                    FilledTonalButton(onClick = { onSetStatus(status) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(statusActionIcon(status), contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(statusActionLabel(status))
                    }
                }
            }
            if (photo.isDuplicateGroup != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete this duplicate", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showFullScreen) {
        FullScreenPhotoViewer(photo = photo, onDismiss = { showFullScreen = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this photo?") },
            text = { Text("This removes the file from your device and can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteDuplicate()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FullScreenPhotoViewer(photo: Photo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
