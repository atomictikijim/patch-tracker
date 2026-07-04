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
import androidx.compose.foundation.lazy.items
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
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(backup.awards) { award ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${award.playerName} · #${award.playerNumber} · Div ${award.division} · ${award.dateEarned.formatted()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                award.patches.forEach { patch ->
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
                                        StatusBadge(awarded = patch.awardedAtTime || patch.fulfilledDate != null)
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
