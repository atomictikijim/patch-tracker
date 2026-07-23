package com.prolocity.patchtracker.ui.sessions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.Session
import com.prolocity.patchtracker.data.buildSessionBackupData
import com.prolocity.patchtracker.data.writeSessionBackup
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.SaveButton
import com.prolocity.patchtracker.ui.components.SessionFormDialog
import com.prolocity.patchtracker.ui.components.cleanUpOrphanedAwardPhotos
import com.prolocity.patchtracker.ui.components.formatted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: PatchTrackerViewModel,
    sessionId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val patchAwards by viewModel.patchAwards.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()

    var session by remember { mutableStateOf<Session?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCantDeleteCurrentDialog by remember { mutableStateOf(false) }
    var showMustFinalizeDialog by remember { mutableStateOf(false) }
    var showExportBlockedDialog by remember { mutableStateOf(false) }

    // Exporting carries this session's still-owed patches into the current session. That needs a
    // *different* current session to move them into — if this session is itself the current one (or
    // there's no current session), export is blocked until the user starts/sets the next session.
    val carryTarget = currentSession?.takeIf { it.id != sessionId }

    LaunchedEffect(sessionId) {
        session = viewModel.getSession(sessionId)
    }

    val awardCount = remember(patchAwards, sessionId) {
        patchAwards.filter { it.sessionId == sessionId }.map { it.eventId }.distinct().size
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val current = session
        if (uri == null || current == null) return@rememberLauncherForActivityResult
        val target = carryTarget
        coroutineScope.launch {
            val lines = viewModel.getSessionAwardLines(sessionId)
            val data = buildSessionBackupData(current, lines)
            context.contentResolver.openOutputStream(uri)?.use { writeSessionBackup(it, data) }
            // Backup holds the full record (owed + awarded); now clear the awarded ones and carry the
            // owed ones into the current session, then lock this session. Joining the launched Job
            // (rather than just firing it) matters here - the cleanup right after must see the DB
            // state as it is *after* finalize's deletes, not before.
            if (target != null) {
                viewModel.finalizeSessionCarryingOwed(sessionId, target.id).join()
            } else {
                viewModel.markSessionFinalized(sessionId).join()
            }
            session = current.copy(isFinalized = true)
            val referenced = viewModel.getAllAwardPhotoPaths()
            withContext(Dispatchers.IO) { cleanUpOrphanedAwardPhotos(context, referenced) }
        }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = session?.name ?: "Session",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (session?.isFinalized != true) {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename")
                        }
                    }
                    IconButton(onClick = {
                        when {
                            session?.isCurrent == true -> showCantDeleteCurrentDialog = true
                            session?.isFinalized != true -> showMustFinalizeDialog = true
                            else -> showDeleteDialog = true
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        val current = session ?: return@Scaffold

        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Created ${current.createdDate.formatted()}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (awardCount == 1) "1 patch award entry" else "$awardCount patch award entries",
                style = MaterialTheme.typography.bodyMedium
            )
            if (current.isFinalized) {
                Text(
                    text = "Finalized — exported and locked from further changes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!current.isCurrent && !current.isFinalized) {
                SaveButton(
                    label = "Set as Current",
                    enabled = true,
                    onClick = { viewModel.setCurrentSession(sessionId) }
                )
            }

            if (!current.isFinalized) {
                Text(
                    text = "Exporting locks this session. Patches still owed carry over to the current " +
                        "session; patches already awarded are cleared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {
                    if (current.isFinalized || carryTarget != null) {
                        exportLauncher.launch("PatchTracker_${current.name}_${LocalDate.now()}.zip")
                    } else {
                        showExportBlockedDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export Backup") }

            OutlinedButton(
                onClick = { showClearDialog = true },
                enabled = awardCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear Patches for This Session") }
        }
    }

    if (showRenameDialog) {
        session?.let { current ->
            SessionFormDialog(
                title = "Rename Session",
                initialName = current.name,
                onSave = { name ->
                    if (name.isNotBlank()) {
                        viewModel.renameSession(current, name)
                        session = current.copy(name = name)
                    }
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear this session's patches?",
            text = "This deletes every patch award entry in this session. Export a backup first if you want to keep a copy. This cannot be undone.",
            onConfirm = {
                viewModel.clearSessionAwards(sessionId)
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete session?",
            text = "This deletes the session and any remaining patch award entries in it. This cannot be undone.",
            onConfirm = {
                session?.let { viewModel.deleteSession(it) }
                showDeleteDialog = false
                onBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showExportBlockedDialog) {
        ConfirmDialog(
            title = "Start the next session first",
            text = "This is the current session, so there's nowhere to carry its still-owed patches. " +
                "Start a new session (or set another as current) first, then export this one.",
            confirmLabel = "OK",
            onConfirm = { showExportBlockedDialog = false },
            onDismiss = { showExportBlockedDialog = false }
        )
    }

    if (showMustFinalizeDialog) {
        ConfirmDialog(
            title = "Export this session first",
            text = "A session must be exported (finalizing it) before it can be deleted.",
            confirmLabel = "OK",
            onConfirm = { showMustFinalizeDialog = false },
            onDismiss = { showMustFinalizeDialog = false }
        )
    }

    if (showCantDeleteCurrentDialog) {
        ConfirmDialog(
            title = "Can't delete the current session",
            text = "Set a different session as current first, then delete this one.",
            confirmLabel = "OK",
            onConfirm = { showCantDeleteCurrentDialog = false },
            onDismiss = { showCantDeleteCurrentDialog = false }
        )
    }
}
