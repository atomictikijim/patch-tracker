package com.prolocity.patchtracker.ui.teams

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prolocity.patchtracker.data.ImportSummary
import com.prolocity.patchtracker.data.MAX_TEAM_PLAYERS
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.CsvImportResultDialog
import com.prolocity.patchtracker.ui.components.CSV_MIME_TYPES
import com.prolocity.patchtracker.ui.components.HelpAction
import com.prolocity.patchtracker.ui.components.InitialsAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamListScreen(
    viewModel: PatchTrackerViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    var divisionFilter by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var importResult by remember { mutableStateOf<ImportSummary?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            if (text != null) viewModel.importTeamsCsv(text) { importResult = it }
        }
    }

    // Divisions still present in the current team set, plus the current selection unioned back in
    // so it stays visible/clearable even if it now matches nothing.
    val divisionOptions = teams
        .map { it.team.division }
        .let { (it + listOfNotNull(divisionFilter)).distinct().sorted() }
        .map { it to if (it.isBlank()) "No division" else "Division $it" }

    val visibleTeams = teams.filter { divisionFilter == null || it.team.division == divisionFilter }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = "Teams",
                actions = {
                    IconButton(onClick = { importLauncher.launch(CSV_MIME_TYPES) }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "Import teams from CSV")
                    }
                    HelpAction("Teams")
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add team")
            }
        }
    ) { innerPadding ->
        if (teams.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No teams yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                DivisionFilterDropdown(
                    options = divisionOptions,
                    selected = divisionFilter,
                    onSelected = { divisionFilter = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (visibleTeams.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No teams in this division.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(visibleTeams, key = { it.team.id }) { teamWithMembers ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditClick(teamWithMembers.team.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InitialsAvatar(name = teamWithMembers.team.name)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(teamWithMembers.team.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Division ${teamWithMembers.team.division} · ${teamWithMembers.members.size}/$MAX_TEAM_PLAYERS players",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    importResult?.let { summary ->
        CsvImportResultDialog(
            title = "Team Import",
            noun = "team",
            summary = summary,
            onDismiss = { importResult = null }
        )
    }
}

// "Filter by division or all" dropdown for the Teams list. [options] are (value, displayLabel)
// pairs; a null selection means no filter (shows "All Divisions").
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DivisionFilterDropdown(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = if (selected == null) "All Divisions"
        else options.firstOrNull { it.first == selected }?.second ?: "All Divisions"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Division") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All Divisions") },
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
