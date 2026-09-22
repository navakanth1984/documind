package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.SyncStatus
import com.example.ui.viewmodel.DocuMindViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: DocuMindViewModel,
    onBack: () -> Unit
) {
    val allDocs by viewModel.allDocuments.collectAsState()
    val isSyncing by viewModel.isSyncingCloud.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val isSignedIn by viewModel.isUserSignedIn.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncEventsLog by viewModel.syncEventsLog.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val deviceId = viewModel.deviceId
    val effectiveUserId = viewModel.effectiveUserId

    val offlineCount = allDocs.count { it.isOfflinePrivacy }
    val syncedCount = allDocs.count { it.isCloudSynced && !it.isOfflinePrivacy }
    val pendingCount = allDocs.count { !it.isCloudSynced && !it.isOfflinePrivacy }

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cloud Sync & Backup Center",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Firebase Firestore Real-Time Cross-Device Sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("sync_screen_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Real-Time Sync Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (syncStatus) {
                        is SyncStatus.LiveListening -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        is SyncStatus.Syncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        is SyncStatus.Idle -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (syncStatus) {
                                            is SyncStatus.LiveListening -> Color(0xFF16A34A)
                                            is SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
                                            is SyncStatus.Error -> MaterialTheme.colorScheme.error
                                            is SyncStatus.Idle -> Color(0xFF64748B)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (syncStatus) {
                                    is SyncStatus.LiveListening -> "Live Snapshot Listener Active"
                                    is SyncStatus.Syncing -> "Cloud Synchronizing..."
                                    is SyncStatus.Error -> "Connection Issue"
                                    is SyncStatus.Idle -> "Cloud Backup Ready"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            modifier = Modifier.testTag("auto_sync_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAutoSyncEnabled) {
                            "Real-time bidirectional synchronization is active. Document scans, OCR updates, and note edits in local Room DB sync automatically to Firestore and mirror across your devices."
                        } else {
                            "Real-time sync paused. Documents will only sync to Firestore when you trigger a manual backup."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Firebase Auth Account Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isSignedIn) "Firebase Account" else "Cloud Sync Session",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Device UID: ${effectiveUserId.take(16)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isSignedIn) {
                        Button(
                            onClick = { viewModel.signInUser("DocuMind Scholar") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_sign_in_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Firebase Cloud Account", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authenticated Session", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { viewModel.signInUser("DocuMind Scholar") }
                            ) {
                                Text("Re-authenticate")
                            }
                        }
                    }
                }
            }

            // Sync Status & Operations Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Room & Firestore Synchronization",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncMetricBox(
                            label = "Cloud Synced",
                            value = "$syncedCount",
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        SyncMetricBox(
                            label = "Pending Sync",
                            value = "$pendingCount",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        SyncMetricBox(
                            label = "Offline Private",
                            value = "$offlineCount",
                            color = Color(0xFFB45309),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dual Action Buttons: Backup Now & Restore from Cloud
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncCloudNow() },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sync_cloud_now_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.restoreFromCloud() },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("restore_from_cloud_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Cloud", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    syncMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Connection & Firestore Target Specs
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firestore Cloud Configuration",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ConfigSpecRow(label = "Local Device ID", value = deviceId)
                    ConfigSpecRow(label = "Firestore Collection", value = "users/$effectiveUserId/documents")
                    ConfigSpecRow(
                        label = "Last Cloud Backup",
                        value = lastSyncTimestamp?.let { dateFormat.format(Date(it)) } ?: "Not yet synced"
                    )
                    ConfigSpecRow(label = "Protocol", value = "Firestore Snapshot Listeners (Real-time)")
                }
            }

            // Live Firestore Activity Feed
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Real-Time Sync Activity Feed",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "${syncEventsLog.size} events",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (syncEventsLog.isEmpty()) {
                        Text(
                            text = "No sync activity recorded yet. Perform a scan or trigger a cloud backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            syncEventsLog.takeLast(6).reversed().forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (log.isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (log.isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (log.isError) MaterialTheme.colorScheme.error else Color(0xFF16A34A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.message,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Offline Privacy Guarantee Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF92400E))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Processing & Zero-Knowledge Privacy",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF92400E)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Documents flagged as confidential or offline are stored exclusively in your device's local Room Database (SQLite). The CloudSyncManager filters them out prior to constructing any Firestore payload, guaranteeing zero cloud transmission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F),
                        lineHeight = 18.sp
                    )
                }
            }

            // Multiplatform & Cross-Platform Architecture Note
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Multi-Device Architecture & Tombstone Deletes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "DocuMind synchronizes Room entities with Firestore documents using timestamp-based reconciliation. When a document is removed on one device, a tombstone event propagates across all connected listeners to maintain consistent state without data loss.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SyncMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
        }
    }
}
