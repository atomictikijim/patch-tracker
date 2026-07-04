package com.prolocity.patchtracker.ui.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.MAX_TEAM_PLAYERS
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.data.Team
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.SaveButton
import com.prolocity.patchtracker.ui.components.SectionLabel
import com.prolocity.patchtracker.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamEditScreen(
    viewModel: PatchTrackerViewModel,
    teamId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val isNew = teamId == Routes.NEW_ID
    val players by viewModel.players.collectAsStateWithLifecycle()

    var loaded by remember { mutableStateOf(isNew) }
    var isEditing by remember { mutableStateOf(isNew) }
    var existing by remember { mutableStateOf<Team?>(null) }
    var name by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("") }
    var slotPlayerIds by remember { mutableStateOf<List<Long?>>(List(MAX_TEAM_PLAYERS) { null }) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        if (!isNew) {
            val team = viewModel.getTeam(teamId)
            existing = team
            if (team != null) {
                name = team.name
                division = team.division
            }
            val orderedIds = viewModel.getTeamMemberIds(teamId)
            slotPlayerIds = List(MAX_TEAM_PLAYERS) { index -> orderedIds.getOrNull(index) }
            loaded = true
        }
    }

    val canSave = name.isNotBlank() && division.isNotBlank()

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = if (isNew) "Add Team" else if (isEditing) "Edit Team" else name,
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
            if (isEditing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = division,
                    onValueChange = { division = it },
                    label = { Text("Division") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Players")
                    for (slot in 0 until MAX_TEAM_PLAYERS) {
                        val chosenElsewhere = slotPlayerIds.filterIndexed { i, _ -> i != slot }.filterNotNull().toSet()
                        val availablePlayers = players.filter { it.id !in chosenElsewhere }
                        val selectedPlayer = players.find { it.id == slotPlayerIds.getOrNull(slot) }
                        PlayerSlotDropdown(
                            label = if (slot == 0) "Player 1 (Captain)" else "Player ${slot + 1}",
                            players = availablePlayers,
                            selected = selectedPlayer,
                            onSelected = { player ->
                                slotPlayerIds = slotPlayerIds.toMutableList().also { it[slot] = player?.id }
                            }
                        )
                    }
                }

                SaveButton(
                    enabled = canSave,
                    onClick = {
                        if (isNew) {
                            viewModel.addTeam(name.trim(), division.trim(), slotPlayerIds)
                            onDone()
                        } else {
                            existing?.let {
                                viewModel.updateTeam(it.copy(name = name.trim(), division = division.trim()), slotPlayerIds)
                            }
                            isEditing = false
                        }
                    }
                )
            } else {
                Column {
                    SectionLabel("Team")
                    Text(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text("Division $division", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column {
                    SectionLabel("Players")
                    val filled = slotPlayerIds.withIndex().mapNotNull { (slot, id) ->
                        players.find { it.id == id }?.let { slot to it }
                    }
                    if (filled.isEmpty()) {
                        Text(
                            "No players assigned yet.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filled.forEach { (slot, player) ->
                            Text(
                                "${player.name} (#${player.playerNumber})" + if (slot == 0) " — Captain" else "",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                SaveButton(
                    enabled = true,
                    onClick = { isEditing = true },
                    label = "Edit Team"
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete team?",
            text = "This cannot be undone.",
            onConfirm = {
                existing?.let { viewModel.deleteTeam(it) }
                showDeleteDialog = false
                onDone()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSlotDropdown(
    label: String,
    players: List<Player>,
    selected: Player?,
    onSelected: (Player?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.name} (#${it.playerNumber})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("None") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
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
