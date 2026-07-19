package com.prolocity.patchtracker.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.components.BrandTopAppBar
import com.prolocity.patchtracker.ui.components.PatchIcon
import com.prolocity.patchtracker.ui.components.RepeatBadge
import com.prolocity.patchtracker.ui.components.StatusBadge
import com.prolocity.patchtracker.ui.components.formatted
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionReviewScreen(
    viewModel: PatchTrackerViewModel,
    onClose: () -> Unit
) {
    val data by viewModel.reviewBackup.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = data?.sessionName ?: "Session Review",
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearReviewBackup()
                        onClose()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                }
            )
        }
    ) { innerPadding ->
        val backup = data ?: return@Scaffold

        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Created ${backup.createdDate.formatted()} · Exported ${backup.exportedAt.formatted()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (backup.awards.isEmpty()) {
                Text(
                    "This backup has no patch award entries.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // Flag repeat patches: within this session, the same player earning the same
                // patch type more than once in the same division. First award (earliest date) is
                // unflagged; later ones are marked. Same patch in a different division is separate.
                // Keyed by (award index, patch index) since the backup carries no line/event ids.
                // Owed patches carried in from a prior session (earned before this session was
                // created) are excluded — they neither get flagged nor flag a new same-session award.
                val repeatRefs = remember(backup.awards, backup.createdDate) {
                    backup.awards
                        .flatMapIndexed { ai, award ->
                            if (award.dateEarned.isBefore(backup.createdDate)) emptyList()
                            else award.patches.mapIndexed { pi, patch ->
                                Triple(ai to pi, award.dateEarned, listOf(award.playerNumber, award.division, patch.name))
                            }
                        }
                        .groupBy { it.third }
                        .flatMap { (_, refs) -> refs.sortedBy { it.second }.drop(1) }
                        .map { it.first }
                        .toSet()
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    itemsIndexed(backup.awards) { awardIndex, award ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val divisionText = if (award.division.isBlank()) "No division" else "Div ${award.division}"
                                Text(
                                    text = "${award.playerName} · #${award.playerNumber} · $divisionText · ${award.dateEarned.formatted()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                award.patches.forEachIndexed { patchIndex, patch ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PatchIcon(
                                            name = patch.name,
                                            iconKey = patch.iconKey,
                                            badgeText = patch.badgeText,
                                            imagePath = patch.imagePath,
                                            size = 24.dp
                                        )
                                        Text(patch.name, modifier = Modifier.weight(1f))
                                        if ((awardIndex to patchIndex) in repeatRefs) {
                                            RepeatBadge()
                                        }
                                        StatusBadge(status = patch.status)
                                    }
                                }
                            }
                            if (!award.photoPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = File(award.photoPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
