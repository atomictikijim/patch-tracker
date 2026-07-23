package com.prolocity.patchtracker.ui.patches

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.prolocity.patchtracker.data.PatchAwardEvent
import com.prolocity.patchtracker.data.PatchAwardLineDetails
import com.prolocity.patchtracker.data.PatchLineStatus
import com.prolocity.patchtracker.data.Session
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.AboutAction
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.ConfirmDialog
import com.prolocity.patchtracker.ui.components.DateBadge
import com.prolocity.patchtracker.ui.components.HelpAction
import com.prolocity.patchtracker.ui.components.PatchIcon
import com.prolocity.patchtracker.ui.components.RepeatBadge
import com.prolocity.patchtracker.ui.components.SettingsAction
import com.prolocity.patchtracker.ui.components.StatusBadge
import com.prolocity.patchtracker.ui.components.formatted
import java.io.File
import java.time.LocalDate

private enum class StatusFilter(val label: String) { ALL("All"), AWARDED("Awarded"), OWED("Owed"), RAFFLE("Raffle") }

internal data class PatchEventGroup(
    val eventId: Long,
    val playerId: Long,
    val playerName: String,
    val playerNumber: String,
    val sessionId: Long,
    val sessionName: String,
    val sessionFinalized: Boolean,
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
    val context = LocalContext.current
    val patchAwards by viewModel.patchAwards.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(StatusFilter.ALL) }
    var sessionFilterId by remember { mutableStateOf<Long?>(null) }
    var sessionFilterTouched by remember { mutableStateOf(false) }
    // Additional, independently-combinable filters (null = no filter on that field).
    var divisionFilter by remember { mutableStateOf<String?>(null) }
    var playerFilter by remember { mutableStateOf<Long?>(null) }
    var dateFilter by remember { mutableStateOf<LocalDate?>(null) }
    var pendingDelete by remember { mutableStateOf<PatchEventGroup?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedEventIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingShareGroups by remember { mutableStateOf<List<PatchEventGroup>?>(null) }
    var pendingShareText by remember { mutableStateOf("") }

    fun exitSelection() {
        selectionMode = false
        selectedEventIds = emptySet()
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    LaunchedEffect(currentSession) {
        if (!sessionFilterTouched) sessionFilterId = currentSession?.id
    }

    // Division/Player/Date options are scoped to the selected session, so clear those filters
    // when the session changes to avoid a selection dangling on values no longer offered.
    LaunchedEffect(sessionFilterId) {
        divisionFilter = null
        playerFilter = null
        dateFilter = null
    }

    // A patch line is a "repeat" if the same player earned the same patch type more than once
    // within the same session AND division. The earliest line per (player, patchType, session,
    // division) is the first award; every later one is flagged. Same patch in a different division
    // is a separate first award, so division is part of the key. Computed across all award lines
    // (not just within one event) so repeats spanning separate award entries are caught too.
    // Owed patches carried in from a prior session are excluded entirely: they don't get flagged and
    // don't count as a prior award that would flag a genuinely-new one earned this session.
    val repeatLineIds = remember(patchAwards) {
        patchAwards
            .filterNot { it.isCarriedOver }
            .groupBy { listOf(it.playerId, it.patchTypeId, it.sessionId, it.division) }
            .flatMap { (_, lines) ->
                lines
                    .sortedWith(compareBy({ it.dateEarned }, { it.eventId }, { it.lineId }))
                    .drop(1)
                    .map { it.lineId }
            }
            .toSet()
    }

    val groups = remember(patchAwards) {
        patchAwards.groupBy { it.eventId }.map { (eventId, lines) ->
            val first = lines.first()
            PatchEventGroup(
                eventId = eventId,
                playerId = first.playerId,
                playerName = first.playerName,
                playerNumber = first.playerNumber,
                sessionId = first.sessionId,
                sessionName = first.sessionName,
                sessionFinalized = first.sessionFinalized,
                division = first.division,
                dateEarned = first.dateEarned,
                photoPath = first.photoPath,
                lines = lines
            )
        }.sortedWith(
            compareByDescending<PatchEventGroup> { it.dateEarned }
                .thenBy { it.division }
                .thenBy { it.playerName }
        )
    }

    // Group-level filter predicates, reused for both the visible list and to narrow each
    // dropdown's options.
    fun matchesStatus(g: PatchEventGroup) = when (filter) {
        StatusFilter.ALL -> true
        StatusFilter.AWARDED -> g.lines.any { it.status == PatchLineStatus.AWARDED }
        StatusFilter.OWED -> g.lines.any { it.status == PatchLineStatus.OWED }
        StatusFilter.RAFFLE -> g.lines.any { it.status == PatchLineStatus.RAFFLE }
    }
    fun matchesSession(g: PatchEventGroup) = sessionFilterId == null || g.sessionId == sessionFilterId
    fun matchesDivision(g: PatchEventGroup) = divisionFilter == null || g.division == divisionFilter
    fun matchesPlayer(g: PatchEventGroup) = playerFilter == null || g.playerId == playerFilter
    fun matchesDate(g: PatchEventGroup) = dateFilter == null || g.dateEarned == dateFilter

    // Each dropdown offers only values still present once the OTHER active filters are applied
    // (its own filter excluded), so the filters progressively narrow one another. The current
    // selection is unioned back in so it stays visible/clearable even if it now matches nothing.
    val divisionOptions = groups
        .filter { matchesSession(it) && matchesPlayer(it) && matchesDate(it) && matchesStatus(it) }
        .map { it.division }
        .let { (it + listOfNotNull(divisionFilter)).distinct().sorted() }
        .map { it to if (it.isBlank()) "No division" else it }
    val playerOptions = groups
        .filter { matchesSession(it) && matchesDivision(it) && matchesDate(it) && matchesStatus(it) }
        .let { passing -> passing + listOfNotNull(playerFilter?.let { pid -> groups.firstOrNull { it.playerId == pid } }) }
        .distinctBy { it.playerId }
        .sortedBy { it.playerName }
        .map { it.playerId to "${it.playerName} (#${it.playerNumber})" }
    val dateOptions = groups
        .filter { matchesSession(it) && matchesDivision(it) && matchesPlayer(it) && matchesStatus(it) }
        .map { it.dateEarned }
        .let { (it + listOfNotNull(dateFilter)).distinct().sortedDescending() }
        .map { it to it.formatted() }

    val filtered = remember(groups, filter, sessionFilterId, divisionFilter, playerFilter, dateFilter) {
        groups.mapNotNull { group ->
            if (!matchesSession(group) || !matchesDivision(group) || !matchesPlayer(group) || !matchesDate(group)) {
                return@mapNotNull null
            }
            val matching = when (filter) {
                StatusFilter.ALL -> group.lines
                StatusFilter.AWARDED -> group.lines.filter { it.status == PatchLineStatus.AWARDED }
                StatusFilter.OWED -> group.lines.filter { it.status == PatchLineStatus.OWED }
                StatusFilter.RAFFLE -> group.lines.filter { it.status == PatchLineStatus.RAFFLE }
            }
            if (matching.isEmpty()) null else group.copy(lines = matching)
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                BrandTopAppBar(
                    title = "${selectedEventIds.size} selected",
                    navigationIcon = {
                        IconButton(onClick = { exitSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = selectedEventIds.isNotEmpty(),
                            onClick = {
                                val selected = groups.filter { it.eventId in selectedEventIds }
                                pendingShareGroups = selected
                                pendingShareText = buildShareSummary(selected, teams, repeatLineIds)
                                exitSelection()
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share selected awards")
                        }
                    }
                )
            } else {
                BrandTopAppBar(
                    title = "Patch Tracker",
                    actions = {
                        if (groups.isNotEmpty()) {
                            IconButton(onClick = { selectionMode = true }) {
                                Icon(Icons.Filled.Checklist, contentDescription = "Select awards to share")
                            }
                        }
                        SettingsAction()
                        HelpAction("Patches")
                        AboutAction()
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Filled.Add, contentDescription = "Add patch award")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SessionFilterDropdown(
                sessions = sessions,
                selectedId = sessionFilterId,
                onSelected = { id ->
                    sessionFilterTouched = true
                    sessionFilterId = id
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Division",
                    allLabel = "All",
                    options = divisionOptions,
                    selected = divisionFilter,
                    onSelected = { divisionFilter = it },
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    label = "Date Earned",
                    allLabel = "All",
                    options = dateOptions,
                    selected = dateFilter,
                    onSelected = { dateFilter = it },
                    modifier = Modifier.weight(1f)
                )
            }

            FilterDropdown(
                label = "Player",
                allLabel = "All",
                options = playerOptions,
                selected = playerFilter,
                onSelected = { playerFilter = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

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
                        val selected = group.eventId in selectedEventIds
                        fun toggle() {
                            selectedEventIds =
                                if (selected) selectedEventIds - group.eventId
                                else selectedEventIds + group.eventId
                        }
                        PatchEventRow(
                            group = group,
                            repeatLineIds = repeatLineIds,
                            selectionMode = selectionMode,
                            selected = selected,
                            onClick = {
                                if (selectionMode) toggle() else onEditClick(group.eventId)
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedEventIds = setOf(group.eventId)
                                }
                            },
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
                        sessionId = group.sessionId,
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

    pendingShareGroups?.let { groupsToShare ->
        ShareSummaryDialog(
            initialText = pendingShareText,
            onDismiss = { pendingShareGroups = null },
            onShare = { finalText ->
                sharePatchAwards(context, groupsToShare, finalText)
                pendingShareGroups = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PatchEventRow(
    group: PatchEventGroup,
    repeatLineIds: Set<Long>,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkFulfilled: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        }
        DateBadge(date = group.dateEarned)

        Column(modifier = Modifier.weight(1f)) {
            val divisionText = if (group.division.isBlank()) "No division" else "Div ${group.division}"
            Text(
                text = "${group.playerName} · #${group.playerNumber} · $divisionText",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Session: ${group.sessionName}",
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
                    if (line.lineId in repeatLineIds) {
                        RepeatBadge()
                    }
                    StatusBadge(status = line.status)
                    if (line.isOutstanding && !group.sessionFinalized && !selectionMode) {
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

        if (!group.sessionFinalized && !selectionMode) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionFilterDropdown(
    sessions: List<Session>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = sessions.find { it.id == selectedId }?.name ?: "All Sessions"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Session") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Sessions") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = { Text(session.name) },
                    onClick = {
                        onSelected(session.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Generic "filter by one value or all" dropdown. [options] are (value, displayLabel) pairs;
// a null selection means no filter (shows [allLabel]). Used for the Division, Player, and
// Date Earned filters, which combine with the session and status filters.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterDropdown(
    label: String,
    allLabel: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = if (selected == null) allLabel
        else options.firstOrNull { it.first == selected }?.second ?: allLabel
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
