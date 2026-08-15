package com.antoine.photobookorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
        topBar = {
            TopAppBar(title = { Text(project.name, fontWeight = FontWeight.Bold) })
        },
        snackbarHost = {
            message?.let { Snackbar(modifier = Modifier.padding(12.dp)) { Text(it) } }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(onClick = { viewModel.scanInbox(project) }) { Text("Scan Inbox") }
                FilledTonalButton(onClick = { viewModel.scanEditedReturn(project) }) { Text("Scan Returns") }
                FilledTonalButton(onClick = { viewModel.exportFinals(project) }) { Text("Export") }
            }

            ScrollableFilterRow(selected = filter, onSelect = { filter = it })

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
            }
        )
    }
}

@Composable
private fun ScrollableFilterRow(selected: PhotoStatus?, onSelect: (PhotoStatus?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    PhotoStatus.PLACED -> "PLACED"
    PhotoStatus.NEEDS_EDIT -> "EDIT"
    PhotoStatus.EDITED -> "EDITED"
    PhotoStatus.FINAL -> "FINAL"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoActionSheet(photo: Photo, onDismiss: () -> Unit, onSetStatus: (PhotoStatus) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Fit
            )
            Text(
                photo.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            PhotoStatus.entries.forEach { status ->
                TextButton(onClick = { onSetStatus(status) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Mark as ${status.name.replace('_', ' ')}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
