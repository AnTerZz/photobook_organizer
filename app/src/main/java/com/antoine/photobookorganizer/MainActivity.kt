package com.antoine.photobookorganizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUris = extractSharedImageUris(intent)

        setContent {
            PhotobookOrganizerTheme {
                val navController = rememberNavController()
                var pendingShare by remember { mutableStateOf(sharedUris) }

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
                        onDone = { pendingShare = emptyList() }
                    )
                }
            }
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
