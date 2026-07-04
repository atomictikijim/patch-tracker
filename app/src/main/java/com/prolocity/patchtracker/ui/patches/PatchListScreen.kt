package com.prolocity.patchtracker.ui.patches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.PatchAwardDetails
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.DateBadge
import com.prolocity.patchtracker.ui.components.StatusBadge
import com.prolocity.patchtracker.ui.components.formatted

private enum class StatusFilter(val label: String) { ALL("All"), AWARDED("Awarded"), OWED("Owed") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchListScreen(
    viewModel: PatchTrackerViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val patchAwards by viewModel.patchAwards.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(StatusFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<PatchAwardDetails?>(null) }

    val filtered = remember(patchAwards, filter) {
        when (filter) {
            StatusFilter.ALL -> patchAwards
            StatusFilter.AWARDED -> patchAwards.filter { !it.isOutstanding }
            StatusFilter.OWED -> patchAwards.filter { it.isOutstanding }
        }
    }

    Scaffold(
        topBar = { BrandTopAppBar(title = "Patch Tracker") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add patch")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (patchAwards.isEmpty()) "No patches logged yet. Tap + to add one." else "No patches match this filter.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(filtered, key = { it.id }) { award ->
                        PatchAwardRow(
                            award = award,
                            onClick = { onEditClick(award.id) },
                            onDeleteClick = { pendingDelete = award },
                            onMarkFulfilled = { viewModel.markFulfilled(award.id) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    pendingDelete?.let { award ->
        ConfirmDialog(
            title = "Delete patch record?",
            text = "This removes the ${award.patchName} record for ${award.playerName}.",
            onConfirm = {
                viewModel.deletePatchAward(
                    com.prolocity.patchtracker.data.PatchAward(
                        id = award.id,
                        playerId = award.playerId,
                        patchTypeId = award.patchTypeId,
                        session = award.session,
                        dateEarned = award.dateEarned,
                        awardedAtTime = award.awardedAtTime,
                        fulfilledDate = award.fulfilledDate
                    )
                )
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun PatchAwardRow(
    award: PatchAwardDetails,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkFulfilled: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DateBadge(date = award.dateEarned)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = award.patchName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${award.playerName} · #${award.playerNumber}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Session: ${award.session}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!award.awardedAtTime && award.fulfilledDate != null) {
                Text(
                    text = "Fulfilled: ${award.fulfilledDate.formatted()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(awarded = !award.isOutstanding)
                if (award.isOutstanding) {
                    TextButton(onClick = onMarkFulfilled) { Text("Mark Fulfilled") }
                }
            }
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
