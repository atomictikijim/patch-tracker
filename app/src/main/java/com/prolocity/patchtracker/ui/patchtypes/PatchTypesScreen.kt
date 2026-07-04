package com.prolocity.patchtracker.ui.patchtypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.PatchTypeFormDialog
import com.prolocity.patchtracker.ui.components.PatchTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchTypesScreen(viewModel: PatchTrackerViewModel) {
    val patchTypes by viewModel.patchTypes.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PatchType?>(null) }
    var pendingDelete by remember { mutableStateOf<PatchType?>(null) }

    Scaffold(
        topBar = { BrandTopAppBar(title = "Patch Types") },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add patch type")
            }
        }
    ) { innerPadding ->
        if (patchTypes.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No patch types yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(patchTypes, key = { it.id }) { patchType ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PatchTypeIcon(patchType = patchType)
                        Text(patchType.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editing = patchType }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = { pendingDelete = patchType }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (showAddDialog) {
        PatchTypeFormDialog(
            title = "Add Patch Type",
            onSave = { name, imagePath ->
                if (name.isNotBlank()) viewModel.addPatchType(name, imagePath)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editing?.let { patchType ->
        PatchTypeFormDialog(
            title = "Edit Patch Type",
            initialName = patchType.name,
            initialImagePath = patchType.imagePath,
            onSave = { name, imagePath ->
                if (name.isNotBlank()) viewModel.updatePatchType(patchType.copy(name = name, imagePath = imagePath))
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    pendingDelete?.let { patchType ->
        ConfirmDialog(
            title = "Delete patch type?",
            text = "This also deletes every patch record using \"${patchType.name}\". This cannot be undone.",
            onConfirm = {
                viewModel.deletePatchType(patchType)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
