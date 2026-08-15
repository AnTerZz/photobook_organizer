package com.antoine.photobookorganizer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.antoine.photobookorganizer.data.Photo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    candidates: List<Photo>,
    onSelect: (Photo) -> Unit,
    onSendToLightroom: (Photo) -> Unit,
    onClose: () -> Unit
) {
    var skippedIds by remember { mutableStateOf(setOf<Long>()) }
    val remaining = remember(candidates, skippedIds) { candidates.filter { it.id !in skippedIds } }
    val current = remaining.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Triage candidates") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (current == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("All caught up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "No more candidates to review right now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                SwipeCard(
                    key = current.id,
                    photo = current,
                    onSwipedRight = { onSelect(current) },
                    onSwipedLeft = { skippedIds = skippedIds + current.id },
                    onSwipedUp = { onSendToLightroom(current) }
                )
            }
        }
    }
}

@Composable
private fun SwipeCard(
    key: Long,
    photo: Photo,
    onSwipedRight: () -> Unit,
    onSwipedLeft: () -> Unit,
    onSwipedUp: () -> Unit
) {
    val offsetX = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val horizontalThreshold = 300f
    val verticalThreshold = 220f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .graphicsLayer { rotationZ = (offsetX.value / 40f).coerceIn(-15f, 15f) }
                .clip(MaterialTheme.shapes.large)
                .pointerInput(key) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val x = offsetX.value
                                val y = offsetY.value
                                when {
                                    y < -verticalThreshold && abs(y) > abs(x) -> {
                                        offsetY.animateTo(-2000f, tween(220))
                                        onSwipedUp()
                                    }
                                    x > horizontalThreshold -> {
                                        offsetX.animateTo(2000f, tween(220))
                                        onSwipedRight()
                                    }
                                    x < -horizontalThreshold -> {
                                        offsetX.animateTo(-2000f, tween(220))
                                        onSwipedLeft()
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, tween(220))
                                        offsetY.animateTo(0f, tween(220))
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        ) {
            FilledIconButton(
                onClick = { scope.launch { offsetX.animateTo(-2000f, tween(220)); onSwipedLeft() } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Skip")
            }
            FilledIconButton(
                onClick = { scope.launch { offsetY.animateTo(-2000f, tween(220)); onSwipedUp() } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Send to Lightroom")
            }
            FilledIconButton(
                onClick = { scope.launch { offsetX.animateTo(2000f, tween(220)); onSwipedRight() } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Select")
            }
        }
    }
}
