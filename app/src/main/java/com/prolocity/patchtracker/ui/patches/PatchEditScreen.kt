package com.prolocity.patchtracker.ui.patches

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.prolocity.patchtracker.data.PatchAwardEvent
import com.prolocity.patchtracker.data.PatchAwardLine
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.data.Session
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.DatePickerField
import com.prolocity.patchtracker.ui.components.PatchTypeFormDialog
import com.prolocity.patchtracker.ui.components.PatchTypeIcon
import com.prolocity.patchtracker.ui.components.SaveButton
import com.prolocity.patchtracker.ui.components.SectionLabel
import com.prolocity.patchtracker.ui.components.createPatchPhotoFile
import com.prolocity.patchtracker.ui.components.patchPhotoUriFor
import com.prolocity.patchtracker.ui.navigation.Routes
import java.io.File
import java.time.LocalDate

private data class PatchLineState(
    val key: Long,
    val lineId: Long,
    val patchType: PatchType?,
    val awardedAtTime: Boolean,
    val fulfilled: Boolean,
    val fulfilledDate: LocalDate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchEditScreen(
    viewModel: PatchTrackerViewModel,
    patchAwardId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val isNew = patchAwardId == Routes.NEW_ID
    val context = LocalContext.current
    val players by viewModel.players.collectAsStateWithLifecycle()
    val patchTypes by viewModel.patchTypes.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()

    var loaded by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<PatchAwardEvent?>(null) }
    var rawLines by remember { mutableStateOf<List<PatchAwardLine>?>(null) }
    var linesInitialized by remember { mutableStateOf(false) }

    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var division by remember { mutableStateOf("") }
    var dateEarned by remember { mutableStateOf(LocalDate.now()) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }

    var lines by remember { mutableStateOf(listOf<PatchLineState>()) }
    var nextKey by remember { mutableStateOf(0L) }
    fun newKey(): Long {
        nextKey += 1
        return nextKey
    }

    var pendingNewPatchTypeName by remember { mutableStateOf<String?>(null) }
    var pendingNewPatchTypeForLineKey by remember { mutableStateOf<Long?>(null) }
    var showAddPatchTypeDialog by remember { mutableStateOf(false) }
    var addPatchTypeForLineKey by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoPath = pendingPhotoPath
    }

    LaunchedEffect(patchAwardId) {
        if (!isNew) {
            val event = viewModel.getPatchAwardEvent(patchAwardId)
            existing = event
            if (event != null) {
                division = event.division
                dateEarned = event.dateEarned
                photoPath = event.photoPath
            }
            rawLines = viewModel.getPatchAwardLines(patchAwardId)
        } else {
            rawLines = emptyList()
        }
        loaded = true
    }

    LaunchedEffect(players, existing) {
        val event = existing
        if (event != null && selectedPlayer == null) {
            selectedPlayer = players.find { it.id == event.playerId }
        } else if (isNew && selectedPlayer == null && players.size == 1) {
            selectedPlayer = players.first()
        }
    }

    LaunchedEffect(sessions, existing, currentSession) {
        val event = existing
        if (event != null && selectedSession == null) {
            selectedSession = sessions.find { it.id == event.sessionId }
        } else if (isNew && selectedSession == null) {
            selectedSession = currentSession
        }
    }

    LaunchedEffect(patchTypes, rawLines, pendingNewPatchTypeForLineKey) {
        val raw = rawLines
        if (raw != null && !linesInitialized && (raw.isEmpty() || patchTypes.isNotEmpty())) {
            lines = if (raw.isEmpty()) {
                listOf(PatchLineState(key = newKey(), lineId = 0, patchType = null, awardedAtTime = true, fulfilled = false, fulfilledDate = LocalDate.now()))
            } else {
                raw.map { line ->
                    PatchLineState(
                        key = newKey(),
                        lineId = line.id,
                        patchType = patchTypes.find { it.id == line.patchTypeId },
                        awardedAtTime = line.awardedAtTime,
                        fulfilled = line.fulfilledDate != null,
                        fulfilledDate = line.fulfilledDate ?: LocalDate.now()
                    )
                }
            }
            linesInitialized = true
        }

        val lineKey = pendingNewPatchTypeForLineKey
        val name = pendingNewPatchTypeName
        if (lineKey != null && name != null) {
            patchTypes.find { it.name.equals(name, ignoreCase = true) }?.let { newType ->
                lines = lines.map { if (it.key == lineKey) it.copy(patchType = newType) else it }
                pendingNewPatchTypeForLineKey = null
                pendingNewPatchTypeName = null
            }
        }
    }

    val canSave = selectedPlayer != null && selectedSession != null && division.isNotBlank() &&
        lines.isNotEmpty() && lines.all { it.patchType != null }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = if (isNew) "Add Patch Award" else "Edit Patch Award",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!loaded) return@Scaffold

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlayerDropdown(
                players = players,
                selected = selectedPlayer,
                onSelected = { selectedPlayer = it }
            )

            SessionDropdown(
                sessions = sessions,
                selected = selectedSession,
                onSelected = { selectedSession = it }
            )

            OutlinedTextField(
                value = division,
                onValueChange = { division = it },
                label = { Text("Division") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = "Date Earned",
                date = dateEarned,
                onDateSelected = { dateEarned = it },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                SectionLabel("Patches")
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    lines.forEachIndexed { index, line ->
                        PatchLineCard(
                            index = index,
                            line = line,
                            patchTypes = patchTypes,
                            canRemove = lines.size > 1,
                            onPatchTypeSelected = { type ->
                                lines = lines.map { if (it.key == line.key) it.copy(patchType = type) else it }
                            },
                            onAddNewPatchType = {
                                addPatchTypeForLineKey = line.key
                                showAddPatchTypeDialog = true
                            },
                            onAwardedAtTimeChanged = { awarded ->
                                lines = lines.map { if (it.key == line.key) it.copy(awardedAtTime = awarded) else it }
                            },
                            onFulfilledChanged = { fulfilled ->
                                lines = lines.map { if (it.key == line.key) it.copy(fulfilled = fulfilled) else it }
                            },
                            onFulfilledDateChanged = { date ->
                                lines = lines.map { if (it.key == line.key) it.copy(fulfilledDate = date) else it }
                            },
                            onRemove = {
                                lines = lines.filterNot { it.key == line.key }
                            }
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        lines = lines + PatchLineState(
                            key = newKey(),
                            lineId = 0,
                            patchType = null,
                            awardedAtTime = true,
                            fulfilled = false,
                            fulfilledDate = LocalDate.now()
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("+ Add Another Patch") }
            }

            Column {
                SectionLabel("Photo")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    val currentPath = photoPath
                    if (!currentPath.isNullOrBlank()) {
                        AsyncImage(
                            model = File(currentPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        }
                    }
                    Column {
                        TextButton(onClick = {
                            val file = createPatchPhotoFile(context)
                            pendingPhotoPath = file.absolutePath
                            cameraLauncher.launch(patchPhotoUriFor(context, file))
                        }) { Text(if (photoPath == null) "Take Photo" else "Retake Photo") }
                        if (photoPath != null) {
                            TextButton(onClick = { photoPath = null }) { Text("Remove Photo") }
                        }
                    }
                }
            }

            SaveButton(
                enabled = canSave,
                onClick = {
                    val event = PatchAwardEvent(
                        id = existing?.id ?: 0,
                        playerId = selectedPlayer!!.id,
                        sessionId = selectedSession!!.id,
                        division = division.trim(),
                        dateEarned = dateEarned,
                        photoPath = photoPath
                    )
                    val awardLines = lines.map { line ->
                        PatchAwardLine(
                            id = line.lineId,
                            eventId = event.id,
                            patchTypeId = line.patchType!!.id,
                            awardedAtTime = line.awardedAtTime,
                            fulfilledDate = if (!line.awardedAtTime && line.fulfilled) line.fulfilledDate else null
                        )
                    }
                    if (isNew) viewModel.addPatchAwardEvent(event, awardLines) else viewModel.updatePatchAwardEvent(event, awardLines)
                    onDone()
                }
            )
        }
    }

    if (showAddPatchTypeDialog) {
        PatchTypeFormDialog(
            title = "Add Patch Type",
            onSave = { name, imagePath ->
                if (name.isNotBlank()) {
                    pendingNewPatchTypeName = name
                    pendingNewPatchTypeForLineKey = addPatchTypeForLineKey
                    viewModel.addPatchType(name, imagePath)
                }
                showAddPatchTypeDialog = false
                addPatchTypeForLineKey = null
            },
            onDismiss = {
                showAddPatchTypeDialog = false
                addPatchTypeForLineKey = null
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete patch award?",
            text = "This removes every patch in this award entry. This cannot be undone.",
            onConfirm = {
                existing?.let { viewModel.deletePatchAwardEvent(it) }
                showDeleteDialog = false
                onDone()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun PatchLineCard(
    index: Int,
    line: PatchLineState,
    patchTypes: List<PatchType>,
    canRemove: Boolean,
    onPatchTypeSelected: (PatchType) -> Unit,
    onAddNewPatchType: () -> Unit,
    onAwardedAtTimeChanged: (Boolean) -> Unit,
    onFulfilledChanged: (Boolean) -> Unit,
    onFulfilledDateChanged: (LocalDate) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Patch ${index + 1}", fontWeight = FontWeight.Bold)
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove patch")
                }
            }
        }

        PatchTypeDropdown(
            patchTypes = patchTypes,
            selected = line.patchType,
            onSelected = onPatchTypeSelected,
            onAddNew = onAddNewPatchType
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = line.awardedAtTime,
                onClick = { onAwardedAtTimeChanged(true) },
                label = { Text("Awarded at the time") }
            )
            FilterChip(
                selected = !line.awardedAtTime,
                onClick = { onAwardedAtTimeChanged(false) },
                label = { Text("Still owed") }
            )
        }

        if (!line.awardedAtTime) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = line.fulfilled, onCheckedChange = onFulfilledChanged)
                Text("Since fulfilled")
            }
            if (line.fulfilled) {
                DatePickerField(
                    label = "Fulfilled Date",
                    date = line.fulfilledDate,
                    onDateSelected = onFulfilledDateChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDropdown(
    players: List<Player>,
    selected: Player?,
    onSelected: (Player) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.name} (#${it.playerNumber})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Player") },
            placeholder = { Text(if (players.isEmpty()) "Add a player first" else "Select a player") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text("${player.name} (#${player.playerNumber})") },
                    onClick = {
                        onSelected(player)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDropdown(
    sessions: List<Session>,
    selected: Session?,
    onSelected: (Session) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Session") },
            placeholder = { Text(if (sessions.isEmpty()) "Add a session first" else "Select a session") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = { Text(session.name) },
                    onClick = {
                        onSelected(session)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatchTypeDropdown(
    patchTypes: List<PatchType>,
    selected: PatchType?,
    onSelected: (PatchType) -> Unit,
    onAddNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Patch") },
            placeholder = { Text("Select a patch") },
            leadingIcon = selected?.let { { PatchTypeIcon(patchType = it, size = 28.dp) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            patchTypes.forEach { patchType ->
                DropdownMenuItem(
                    text = { Text(patchType.name) },
                    leadingIcon = { PatchTypeIcon(patchType = patchType, size = 28.dp) },
                    onClick = {
                        onSelected(patchType)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("+ Add new patch type") },
                onClick = {
                    expanded = false
                    onAddNew()
                }
            )
        }
    }
}
