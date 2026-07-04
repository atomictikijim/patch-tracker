package com.prolocity.patchtracker.ui.patches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.PatchAward
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.DatePickerField
import com.prolocity.patchtracker.ui.components.PatchTypeFormDialog
import com.prolocity.patchtracker.ui.components.PatchTypeIcon
import com.prolocity.patchtracker.ui.components.SaveButton
import com.prolocity.patchtracker.ui.components.SectionLabel
import com.prolocity.patchtracker.ui.navigation.Routes
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchEditScreen(
    viewModel: PatchTrackerViewModel,
    patchAwardId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val isNew = patchAwardId == Routes.NEW_ID
    val players by viewModel.players.collectAsStateWithLifecycle()
    val patchTypes by viewModel.patchTypes.collectAsStateWithLifecycle()

    var loaded by remember { mutableStateOf(isNew) }
    var existing by remember { mutableStateOf<PatchAward?>(null) }

    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var selectedPatchType by remember { mutableStateOf<PatchType?>(null) }
    var session by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("") }
    var dateEarned by remember { mutableStateOf(LocalDate.now()) }
    var awardedAtTime by remember { mutableStateOf(true) }
    var fulfilled by remember { mutableStateOf(false) }
    var fulfilledDate by remember { mutableStateOf(LocalDate.now()) }

    var pendingNewPatchTypeName by remember { mutableStateOf<String?>(null) }
    var showAddPatchTypeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patchAwardId) {
        if (!isNew) {
            val award = viewModel.getPatchAward(patchAwardId)
            existing = award
            if (award != null) {
                session = award.session
                division = award.division
                dateEarned = award.dateEarned
                awardedAtTime = award.awardedAtTime
                fulfilled = award.fulfilledDate != null
                fulfilledDate = award.fulfilledDate ?: LocalDate.now()
            }
            loaded = true
        }
    }

    LaunchedEffect(players, existing) {
        val award = existing
        if (award != null && selectedPlayer == null) {
            selectedPlayer = players.find { it.id == award.playerId }
        } else if (isNew && selectedPlayer == null && players.size == 1) {
            selectedPlayer = players.first()
        }
    }

    LaunchedEffect(patchTypes, existing, pendingNewPatchTypeName) {
        val award = existing
        if (award != null && selectedPatchType == null) {
            selectedPatchType = patchTypes.find { it.id == award.patchTypeId }
        }
        pendingNewPatchTypeName?.let { name ->
            patchTypes.find { it.name.equals(name, ignoreCase = true) }?.let {
                selectedPatchType = it
                pendingNewPatchTypeName = null
            }
        }
    }

    val canSave = selectedPlayer != null && selectedPatchType != null && session.isNotBlank() && division.isNotBlank()

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = if (isNew) "Add Patch" else "Edit Patch",
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

            PatchTypeDropdown(
                patchTypes = patchTypes,
                selected = selectedPatchType,
                onSelected = { selectedPatchType = it },
                onAddNew = { showAddPatchTypeDialog = true }
            )

            OutlinedTextField(
                value = session,
                onValueChange = { session = it },
                label = { Text("Session (e.g. 2026 Summer)") },
                modifier = Modifier.fillMaxWidth()
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
                SectionLabel("Status")
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = awardedAtTime,
                        onClick = { awardedAtTime = true },
                        label = { Text("Awarded at the time") }
                    )
                    FilterChip(
                        selected = !awardedAtTime,
                        onClick = { awardedAtTime = false },
                        label = { Text("Still owed") }
                    )
                }
            }

            if (!awardedAtTime) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = fulfilled, onCheckedChange = { fulfilled = it })
                        Text("Since fulfilled (patch has now been given to the player)")
                    }
                    if (fulfilled) {
                        DatePickerField(
                            label = "Fulfilled Date",
                            date = fulfilledDate,
                            onDateSelected = { fulfilledDate = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            SaveButton(
                enabled = canSave,
                onClick = {
                    val award = PatchAward(
                        id = existing?.id ?: 0,
                        playerId = selectedPlayer!!.id,
                        patchTypeId = selectedPatchType!!.id,
                        session = session.trim(),
                        division = division.trim(),
                        dateEarned = dateEarned,
                        awardedAtTime = awardedAtTime,
                        fulfilledDate = if (!awardedAtTime && fulfilled) fulfilledDate else null
                    )
                    if (isNew) viewModel.addPatchAward(award) else viewModel.updatePatchAward(award)
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
                    viewModel.addPatchType(name, imagePath)
                }
                showAddPatchTypeDialog = false
            },
            onDismiss = { showAddPatchTypeDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete patch record?",
            text = "This cannot be undone.",
            onConfirm = {
                existing?.let { viewModel.deletePatchAward(it) }
                showDeleteDialog = false
                onDone()
            },
            onDismiss = { showDeleteDialog = false }
        )
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
