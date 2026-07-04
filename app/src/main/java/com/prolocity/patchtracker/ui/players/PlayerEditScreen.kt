package com.prolocity.patchtracker.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.SaveButton
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
    var existing by remember { mutableStateOf<Player?>(null) }
    var name by remember { mutableStateOf("") }
    var playerNumber by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playerId) {
        if (!isNew) {
            val player = viewModel.getPlayer(playerId)
            existing = player
            if (player != null) {
                name = player.name
                playerNumber = player.playerNumber
            }
        }
    }

    val canSave = name.isNotBlank() && playerNumber.isNotBlank()

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = if (isNew) "Add Player" else "Edit Player",
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
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                modifier = Modifier.fillMaxWidth()
            )
            SaveButton(
                enabled = canSave,
                onClick = {
                    if (isNew) {
                        viewModel.addPlayer(name.trim(), playerNumber.trim())
                    } else {
                        existing?.let {
                            viewModel.updatePlayer(it.copy(name = name.trim(), playerNumber = playerNumber.trim()))
                        }
                    }
                    onDone()
                }
            )
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
