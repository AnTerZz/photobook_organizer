package com.antoine.photobookorganizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antoine.photobookorganizer.ui.ProjectDetailScreen
import com.antoine.photobookorganizer.ui.ProjectListScreen
import com.antoine.photobookorganizer.ui.ShareTargetScreen
import com.antoine.photobookorganizer.ui.theme.PhotobookOrganizerTheme
import com.antoine.photobookorganizer.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by lazy { MainViewModel(application) }

    // Held outside Compose so onNewIntent (called when the app is already running) can update it -
    // this is what was missing: sharing worked only on a cold start, not when the app was already open.
    private val pendingShareState = mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingShareState.value = extractSharedImageUris(intent)

        setContent {
            PhotobookOrganizerTheme {
                val navController = rememberNavController()
                val pendingShare by pendingShareState

                NavHost(navController = navController, startDestination = "projects") {
                    composable("projects") {
                        ProjectListScreen(
                            viewModel = viewModel,
                            onOpenProject = { id -> navController.navigate("project/$id") }
                        )
                    }
                    composable(
                        "project/{projectId}",
                        arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                        ProjectDetailScreen(viewModel = viewModel, projectId = projectId)
                    }
                }

                if (pendingShare.isNotEmpty()) {
                    ShareTargetScreen(
                        viewModel = viewModel,
                        sharedUris = pendingShare,
                        onDone = { pendingShareState.value = emptyList() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uris = extractSharedImageUris(intent)
        if (uris.isNotEmpty()) {
            pendingShareState.value = uris
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSharedImageUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) listOf(uri) else emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
