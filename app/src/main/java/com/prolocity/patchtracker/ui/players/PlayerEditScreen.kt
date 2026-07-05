package com.prolocity.patchtracker.ui.players

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.PatchIcon
import com.prolocity.patchtracker.ui.components.SaveButton
import com.prolocity.patchtracker.ui.components.SectionLabel
import com.prolocity.patchtracker.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEditScreen(
    viewModel: PatchTrackerViewModel,
    playerId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val isNew = playerId == Routes.NEW_ID
    val players by viewModel.players.collectAsStateWithLifecycle()
    val patchAwards by viewModel.patchAwards.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(isNew) }
    var existing by remember { mutableStateOf<Player?>(null) }
    var name by remember { mutableStateOf("") }
    var playerNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playerId) {
        if (!isNew) {
            val player = viewModel.getPlayer(playerId)
            existing = player
            if (player != null) {
                name = player.name
                playerNumber = player.playerNumber
                phoneNumber = player.phoneNumber.orEmpty()
                email = player.email.orEmpty()
            }
        }
    }

    // Player number is a unique identifier — block saving one another player already holds
    // (excluding this player when editing). Trimmed to match how it's persisted.
    val duplicateNumber = playerNumber.isNotBlank() && players.any {
        it.id != playerId && it.playerNumber.trim().equals(playerNumber.trim(), ignoreCase = true)
    }

    val canSave = name.isNotBlank() && playerNumber.isNotBlank() && !duplicateNumber

    val earnedPatches = remember(patchAwards, playerId) {
        patchAwards
            .filter { it.playerId == playerId }
            .groupBy { it.patchTypeId }
            .map { (_, awards) ->
                val first = awards.first()
                EarnedPatch(first.patchName, first.patchIconKey, first.patchBadgeText, first.patchImagePath, awards.size)
            }
            .sortedBy { it.name }
    }

    val memberTeams = remember(teams, playerId) {
        teams.filter { team -> team.members.any { it.id == playerId } }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = if (isNew) "Add Player" else if (isEditing) "Edit Player" else name,
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
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = playerNumber,
                    onValueChange = { playerNumber = it },
                    label = { Text("Player Number") },
                    isError = duplicateNumber,
                    supportingText = if (duplicateNumber) {
                        { Text("Another player already has this number") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                SaveButton(
                    enabled = canSave,
                    onClick = {
                        if (isNew) {
                            viewModel.addPlayer(
                                name.trim(),
                                playerNumber.trim(),
                                phoneNumber.trim().ifBlank { null },
                                email.trim().ifBlank { null }
                            )
                            onDone()
                        } else {
                            existing?.let {
                                viewModel.updatePlayer(
                                    it.copy(
                                        name = name.trim(),
                                        playerNumber = playerNumber.trim(),
                                        phoneNumber = phoneNumber.trim().ifBlank { null },
                                        email = email.trim().ifBlank { null }
                                    )
                                )
                            }
                            isEditing = false
                        }
                    }
                )
            } else {
                Column {
                    SectionLabel("Player")
                    Text(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text("#$playerNumber", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (phoneNumber.isNotBlank()) {
                        Text(phoneNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (email.isNotBlank()) {
                        Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column {
                    SectionLabel("Patches Earned")
                    if (earnedPatches.isEmpty()) {
                        Text(
                            "No patches earned yet.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        earnedPatches.forEach { patch ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PatchIcon(
                                    name = patch.name,
                                    iconKey = patch.iconKey,
                                    badgeText = patch.badgeText,
                                    imagePath = patch.imagePath,
                                    size = 28.dp
                                )
                                Text(patch.name, modifier = Modifier.weight(1f))
                                Text("×${patch.count}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column {
                    SectionLabel("Teams")
                    if (memberTeams.isEmpty()) {
                        Text(
                            "Not on any team yet.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        memberTeams.forEach { team ->
                            Text(
                                "${team.team.name} — Division ${team.team.division}",
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                SaveButton(
                    enabled = true,
                    onClick = { isEditing = true },
                    label = "Edit Player"
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete player?",
            text = "This also deletes every patch record for this player. This cannot be undone.",
            onConfirm = {
                existing?.let { viewModel.deletePlayer(it) }
                showDeleteDialog = false
                onDone()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

private data class EarnedPatch(
    val name: String,
    val iconKey: String?,
    val badgeText: String?,
    val imagePath: String?,
    val count: Int
)
