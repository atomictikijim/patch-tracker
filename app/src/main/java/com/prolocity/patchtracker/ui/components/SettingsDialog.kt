package com.prolocity.patchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prolocity.patchtracker.PatchTrackerApplication

// A top-bar Settings button, following the AboutAction/HelpAction convention. Scoped to the
// Patches screen only (not every screen, unlike Help/About) since this setting is specifically
// about patch-award-photo storage, not global app configuration.
@Composable
fun SettingsAction() {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings")
    }
    if (show) {
        SettingsDialog(onDismiss = { show = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { (context.applicationContext as PatchTrackerApplication).settings }
    val sdCardDir = remember { availableSdCardDir(context) }
    var useSdCard by remember { mutableStateOf(settings.useSdCardForAwardPhotos) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                topBar = {
                    BrandTopAppBar(
                        title = "Settings",
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close settings")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Patch Award Photos", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Store new award photos on SD card")
                            Text(
                                text = if (sdCardDir != null) {
                                    val freeGb = sdCardDir.usableSpace / (1024.0 * 1024.0 * 1024.0)
                                    "SD card detected — %.1f GB free".format(freeGb)
                                } else {
                                    "No SD card detected on this device"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useSdCard,
                            enabled = sdCardDir != null,
                            onCheckedChange = { checked ->
                                useSdCard = checked
                                settings.useSdCardForAwardPhotos = checked
                            }
                        )
                    }
                    Text(
                        text = "Existing photos are not moved when this changes — only new photos taken " +
                            "or edited after toggling this go to the new location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
