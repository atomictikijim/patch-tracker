package com.prolocity.patchtracker.ui.teams

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.MAX_TEAM_PLAYERS
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
    var existing by remember { mutableStateOf<Team?>(null) }
    var name by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("") }
    var selectedPlayerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        if (!isNew) {
            val teamWithMembers = viewModel.getTeam(teamId)
            existing = teamWithMembers?.team
            if (teamWithMembers != null) {
                name = teamWithMembers.team.name
                division = teamWithMembers.team.division
                selectedPlayerIds = teamWithMembers.members.map { it.id }.toSet()
            }
            loaded = true
        }
    }

    val canSave = name.isNotBlank() && division.isNotBlank()

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = if (isNew) "Add Team" else "Edit Team",
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

            Column {
                SectionLabel("Players (${selectedPlayerIds.size}/$MAX_TEAM_PLAYERS)")
                if (players.isEmpty()) {
                    Text(
                        "No players yet. Add players in the Players tab first.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    players.forEach { player ->
                        val checked = player.id in selectedPlayerIds
                        val canToggleOn = selectedPlayerIds.size < MAX_TEAM_PLAYERS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = checked || canToggleOn) {
                                    selectedPlayerIds = if (checked) {
                                        selectedPlayerIds - player.id
                                    } else {
                                        selectedPlayerIds + player.id
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                enabled = checked || canToggleOn,
                                onCheckedChange = null
                            )
                            Text("${player.name} (#${player.playerNumber})")
                        }
                    }
                }
            }

            SaveButton(
                enabled = canSave,
                onClick = {
                    if (isNew) {
                        viewModel.addTeam(name.trim(), division.trim(), selectedPlayerIds.toList())
                    } else {
                        existing?.let {
                            viewModel.updateTeam(
                                it.copy(name = name.trim(), division = division.trim()),
                                selectedPlayerIds.toList()
                            )
                        }
                    }
                    onDone()
                }
            )
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
