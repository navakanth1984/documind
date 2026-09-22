package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DiagramStudioScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.DocumentListScreen
import com.example.ui.screens.StudyCollabScreen
import com.example.ui.viewmodel.DocuMindViewModel

enum class MainNavTab(val label: String) {
    DOCUMENTS("Documents"),
    STUDY_HUB("Study Hub"),
    DIAGRAMS("Diagram Studio"),
    CLOUD_SYNC("Cloud Sync")
}

@Composable
fun DocuMindMainApp(
    viewModel: DocuMindViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(MainNavTab.DOCUMENTS) }
    var isScanningCamera by remember { mutableStateOf(false) }
    val selectedDoc by viewModel.selectedDocument.collectAsState()

    // Handle back button when viewing a document detail or camera scanner
    BackHandler(enabled = isScanningCamera) {
        isScanningCamera = false
    }

    BackHandler(enabled = !isScanningCamera && selectedDoc != null) {
        viewModel.selectDocument(null)
    }

    Scaffold(
        bottomBar = {
            if (selectedDoc == null && !isScanningCamera) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    MainNavTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val icon = when (tab) {
                            MainNavTab.DOCUMENTS -> Icons.Default.Description
                            MainNavTab.STUDY_HUB -> Icons.Default.School
                            MainNavTab.DIAGRAMS -> Icons.Default.Brush
                            MainNavTab.CLOUD_SYNC -> Icons.Default.CloudSync
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectDocument(null)
                                isScanningCamera = false
                                currentTab = tab
                            },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isScanningCamera) {
                CameraScanScreen(
                    viewModel = viewModel,
                    onBack = { isScanningCamera = false },
                    onScanComplete = { _ ->
                        isScanningCamera = false
                    }
                )
            } else if (selectedDoc != null) {
                DocumentDetailScreen(
                    document = selectedDoc!!,
                    viewModel = viewModel,
                    onBack = { viewModel.selectDocument(null) },
                    onNavigateToCollab = {
                        viewModel.selectDocument(null)
                        currentTab = MainNavTab.STUDY_HUB
                    },
                    onNavigateToDiagram = {
                        viewModel.selectDocument(null)
                        currentTab = MainNavTab.DIAGRAMS
                    }
                )
            } else {
                when (currentTab) {
                    MainNavTab.DOCUMENTS -> {
                        DocumentListScreen(
                            viewModel = viewModel,
                            onDocumentClick = { doc ->
                                viewModel.selectDocument(doc)
                            },
                            onStudyClick = { doc ->
                                viewModel.selectDocument(null)
                                currentTab = MainNavTab.STUDY_HUB
                            },
                            onOpenSyncClick = {
                                currentTab = MainNavTab.CLOUD_SYNC
                            },
                            onOpenScanner = {
                                isScanningCamera = true
                            }
                        )
                    }

                    MainNavTab.STUDY_HUB -> {
                        StudyCollabScreen(
                            viewModel = viewModel,
                            onBack = { currentTab = MainNavTab.DOCUMENTS }
                        )
                    }

                    MainNavTab.DIAGRAMS -> {
                        DiagramStudioScreen(
                            viewModel = viewModel,
                            onBack = { currentTab = MainNavTab.DOCUMENTS }
                        )
                    }

                    MainNavTab.CLOUD_SYNC -> {
                        CloudSyncScreen(
                            viewModel = viewModel,
                            onBack = { currentTab = MainNavTab.DOCUMENTS }
                        )
                    }
                }
            }
        }
    }
}
