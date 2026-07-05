package com.prolocity.patchtracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prolocity.patchtracker.data.Player

// Minimum characters before the player lookup starts showing suggestions.
const val PLAYER_SEARCH_MIN_CHARS = 2

// Type-to-search player field: the user types a name (or number) and, once at least
// PLAYER_SEARCH_MIN_CHARS characters are entered, matching players from [players] appear as
// suggestions. Picking one sets the selection; editing the text again clears it until another
// is picked, so [selected] is only ever a player the user explicitly chose. [players] is the
// candidate list the caller has already narrowed (e.g. by team/division rules) — this field only
// narrows it further by the typed text.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerLookupField(
    label: String,
    players: List<Player>,
    selected: Player?,
    onSelectedChange: (Player?) -> Unit,
    modifier: Modifier = Modifier
) {
    fun display(p: Player) = "${p.name} (#${p.playerNumber})"
    var query by remember { mutableStateOf(selected?.let(::display).orEmpty()) }
    var expanded by remember { mutableStateOf(false) }

    // Reflect an externally-set player (loading an existing record, or auto-select) in the text.
    LaunchedEffect(selected) {
        selected?.let { if (query != display(it)) query = display(it) }
    }

    val matches = remember(query, players, selected) {
        val q = query.trim()
        when {
            q.length < PLAYER_SEARCH_MIN_CHARS -> emptyList()
            // Already showing the chosen player's full label -> nothing to suggest.
            selected != null && q == display(selected) -> emptyList()
            else -> players.filter {
                it.name.contains(q, ignoreCase = true) || it.playerNumber.contains(q, ignoreCase = true)
            }
        }
    }
    val menuOpen = expanded && matches.isNotEmpty()

    ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query,
            onValueChange = { new ->
                query = new
                // Typing invalidates a prior pick until the user chooses again.
                if (selected != null && new != display(selected)) onSelectedChange(null)
                expanded = true
            },
            label = { Text(label) },
            placeholder = {
                Text(if (players.isEmpty()) "No players available" else "Type a name to search")
            },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onSelectedChange(null)
                        expanded = false
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear player")
                    }
                }
            },
            supportingText = {
                if (query.trim().length in 1 until PLAYER_SEARCH_MIN_CHARS) {
                    Text("Type at least $PLAYER_SEARCH_MIN_CHARS characters to search")
                } else if (!menuOpen && selected == null && query.trim().length >= PLAYER_SEARCH_MIN_CHARS) {
                    Text("No players match")
                }
            },
            modifier = modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { expanded = false }) {
            matches.forEach { player ->
                DropdownMenuItem(
                    text = { Text(display(player)) },
                    onClick = {
                        onSelectedChange(player)
                        query = display(player)
                        expanded = false
                    }
                )
            }
        }
    }
}
