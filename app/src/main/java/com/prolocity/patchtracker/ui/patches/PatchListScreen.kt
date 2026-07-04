package com.prolocity.patchtracker.ui.patches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.prolocity.patchtracker.data.PatchAwardEvent
import com.prolocity.patchtracker.data.PatchAwardLineDetails
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.DateBadge
import com.prolocity.patchtracker.ui.components.PatchIcon
import com.prolocity.patchtracker.ui.components.StatusBadge
import com.prolocity.patchtracker.ui.components.formatted
import java.io.File
import java.time.LocalDate

private enum class StatusFilter(val label: String) { ALL("All"), AWARDED("Awarded"), OWED("Owed") }

private data class PatchEventGroup(
    val eventId: Long,
    val playerId: Long,
    val playerName: String,
    val playerNumber: String,
    val session: String,
    val division: String,
    val dateEarned: LocalDate,
    val photoPath: String?,
    val lines: List<PatchAwardLineDetails>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchListScreen(
    viewModel: PatchTrackerViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val patchAwards by viewModel.patchAwards.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(StatusFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<PatchEventGroup?>(null) }

    val groups = remember(patchAwards) {
        patchAwards.groupBy { it.eventId }.map { (eventId, lines) ->
            val first = lines.first()
            PatchEventGroup(
                eventId = eventId,
                playerId = first.playerId,
                playerName = first.playerName,
                playerNumber = first.playerNumber,
                session = first.session,
                division = first.division,
                dateEarned = first.dateEarned,
                photoPath = first.photoPath,
                lines = lines
            )
        }.sortedWith(compareByDescending<PatchEventGroup> { it.dateEarned }.thenBy { it.playerName })
    }

    val filtered = remember(groups, filter) {
        groups.mapNotNull { group ->
            val matching = when (filter) {
                StatusFilter.ALL -> group.lines
                StatusFilter.AWARDED -> group.lines.filter { !it.isOutstanding }
                StatusFilter.OWED -> group.lines.filter { it.isOutstanding }
            }
            if (matching.isEmpty()) null else group.copy(lines = matching)
        }
    }

    Scaffold(
        topBar = { BrandTopAppBar(title = "Patch Tracker") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add patch award")
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
                        text = if (groups.isEmpty()) "No patches logged yet. Tap + to add one." else "No patches match this filter.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(filtered, key = { it.eventId }) { group ->
                        PatchEventRow(
                            group = group,
                            onClick = { onEditClick(group.eventId) },
                            onDeleteClick = { pendingDelete = group },
                            onMarkFulfilled = { lineId -> viewModel.markLineFulfilled(lineId) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    pendingDelete?.let { group ->
        ConfirmDialog(
            title = "Delete patch award?",
            text = "This removes every patch in this award entry for ${group.playerName}.",
            onConfirm = {
                viewModel.deletePatchAwardEvent(
                    PatchAwardEvent(
                        id = group.eventId,
                        playerId = group.playerId,
                        session = group.session,
                        division = group.division,
                        dateEarned = group.dateEarned,
                        photoPath = group.photoPath
                    )
                )
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun PatchEventRow(
    group: PatchEventGroup,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkFulfilled: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DateBadge(date = group.dateEarned)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${group.playerName} · #${group.playerNumber} · Div ${group.division}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Session: ${group.session}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            group.lines.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PatchIcon(
                        name = line.patchName,
                        iconKey = line.patchIconKey,
                        badgeText = line.patchBadgeText,
                        imagePath = line.patchImagePath,
                        size = 24.dp
                    )
                    Text(line.patchName, modifier = Modifier.weight(1f))
                    StatusBadge(awarded = !line.isOutstanding)
                    if (line.isOutstanding) {
                        TextButton(onClick = { onMarkFulfilled(line.lineId) }) { Text("Mark Fulfilled") }
                    }
                }
            }
        }

        if (!group.photoPath.isNullOrBlank()) {
            AsyncImage(
                model = File(group.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
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
