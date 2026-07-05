package com.prolocity.patchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prolocity.patchtracker.data.ImportSummary

// MIME types to offer in the system file picker for a CSV. CSVs get labeled inconsistently across
// devices/apps (text/csv, Excel's type, or a generic octet-stream when the type is unknown), so we
// accept the common ones plus a plain-text wildcard rather than risk hiding the user's file.
val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/vnd.ms-excel",
    "application/octet-stream"
)

// Shows the outcome of a CSV import: how many rows were added, plus any skipped rows and warnings
// (each already a human-readable line from the repository). Shared by the Players and Teams imports.
@Composable
fun CsvImportResultDialog(
    title: String,
    noun: String, // "player"/"team", for the "Added N players" line
    summary: ImportSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val plural = if (summary.added == 1) noun else "${noun}s"
                Text(
                    text = "Added ${summary.added} $plural.",
                    fontWeight = FontWeight.Bold,
                    color = if (summary.added > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )

                if (summary.warnings.isNotEmpty()) {
                    Text(
                        "Warnings (${summary.warnings.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    summary.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }

                if (summary.skipped.isNotEmpty()) {
                    Text(
                        "Skipped (${summary.skipped.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    summary.skipped.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }

                if (summary.added == 0 && summary.skipped.isEmpty() && summary.warnings.isEmpty()) {
                    Text("Nothing to import.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
