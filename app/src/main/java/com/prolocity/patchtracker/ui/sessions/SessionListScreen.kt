package com.prolocity.patchtracker.ui.sessions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.readSessionBackup
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.HelpAction
import com.prolocity.patchtracker.ui.components.SessionFormDialog
import com.prolocity.patchtracker.ui.components.formatted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    viewModel: PatchTrackerViewModel,
    onSessionClick: (Long) -> Unit,
    onReviewOpened: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val openBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val data = readSessionBackup(context, input)
                viewModel.showReviewBackup(data)
                onReviewOpened()
            }
        }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = "Sessions",
                actions = {
                    IconButton(onClick = { openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = "Open backup for review")
                    }
                    HelpAction("Sessions", sections = listOf("Sessions", "Data & backups"))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Start new session")
            }
        }
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions yet. Tap + to start your first session.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionClick(session.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = session.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = session.createdDate.formatted(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (session.isCurrent) {
                            Text(
                                text = "Current",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        } else if (session.isFinalized) {
                            Text(
                                text = "Finalized",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (showAddDialog) {
        SessionFormDialog(
            title = "Start New Session",
            onSave = { name ->
                if (name.isNotBlank()) viewModel.startNewSession(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}
