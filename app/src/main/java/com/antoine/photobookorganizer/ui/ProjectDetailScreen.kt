package com.antoine.photobookorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.antoine.photobookorganizer.data.Photo
import com.antoine.photobookorganizer.data.PhotoStatus
import com.antoine.photobookorganizer.util.BlurDetector
import com.antoine.photobookorganizer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(viewModel: MainViewModel, projectId: Long) {
    val projects by viewModel.projects.collectAsState()
    val project = projects.firstOrNull { it.id == projectId } ?: return
    val photos by viewModel.photosFor(projectId).collectAsState(initial = emptyList())
    val message by viewModel.statusMessage.collectAsState()

    var filter by remember { mutableStateOf<PhotoStatus?>(null) }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }

    val filtered = if (filter == null) photos else photos.filter { it.status == filter }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(project.name) }) },
        snackbarHost = {
            message?.let { Snackbar(modifier = Modifier.padding(8.dp)) { Text(it) } }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { viewModel.scanInbox(project) }) { Text("Scan Inbox") }
                OutlinedButton(onClick = { viewModel.scanEditedReturn(project) }) { Text("Scan Returns") }
                OutlinedButton(onClick = { viewModel.exportFinals(project) }) { Text("Export Finals") }
            }

            ScrollableFilterRow(selected = filter, onSelect = { filter = it })

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { photo ->
                    PhotoThumbnail(photo = photo, onClick = { selectedPhoto = photo })
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
            }
        )
    }
}

@Composable
private fun ScrollableFilterRow(selected: PhotoStatus?, onSelect: (PhotoStatus?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        PhotoStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.name.replace('_', ' ')) }
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: Photo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(modifier = Modifier.align(Alignment.TopStart).padding(2.dp)) {
            if (photo.isDuplicateGroup != null) {
                StatusBadge(text = "DUP", color = Color(0xFFFFA000))
            }
            val blur = photo.blurScore
            if (blur != null && blur < BlurDetector.BLUR_WARNING_THRESHOLD) {
                StatusBadge(text = "BLUR", color = Color(0xFFD32F2F))
            }
        }
        Text(
            text = statusAbbrev(photo.status),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(color, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
}

private fun statusAbbrev(status: PhotoStatus): String = when (status) {
    PhotoStatus.CANDIDATE -> "CAND"
    PhotoStatus.PLACED -> "PLACED"
    PhotoStatus.NEEDS_EDIT -> "EDIT"
    PhotoStatus.EDITED -> "EDITED"
    PhotoStatus.FINAL -> "FINAL"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoActionSheet(photo: Photo, onDismiss: () -> Unit, onSetStatus: (PhotoStatus) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.fileName,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Fit
            )
            Text(photo.fileName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            PhotoStatus.entries.forEach { status ->
                TextButton(onClick = { onSetStatus(status) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark as ${status.name.replace('_', ' ')}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }
        }
    }
}
