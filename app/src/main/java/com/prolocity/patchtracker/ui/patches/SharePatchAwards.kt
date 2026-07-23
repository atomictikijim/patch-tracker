package com.prolocity.patchtracker.ui.patches

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ShareCompat
import com.prolocity.patchtracker.data.PatchLineStatus
import com.prolocity.patchtracker.data.TeamWithMembers
import com.prolocity.patchtracker.ui.components.clearShareCache
import com.prolocity.patchtracker.ui.components.patchPhotoUriFor
import com.prolocity.patchtracker.ui.components.preparePhotoForSharing

// A dialog offering the auto-generated share summary for review/editing before it's actually
// shared - opened from the Patches list's selection-mode Share action, prefilled with
// buildShareSummary's output. Confirming hands the (possibly edited) text to sharePatchAwards.
@Composable
internal fun ShareSummaryDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Share Text", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onShare(text) }) { Text("Share", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Shares the selected patch awards out to another app (typically Facebook), using the given
 * (already reviewed/edited) [text] rather than building it fresh.
 *
 * Facebook removed the Groups API in April 2024, so there's no way to post to a group
 * programmatically — the only route is the system share sheet, where the user picks the group
 * and posts manually. Facebook also strips pre-filled captions from image shares, so the text is
 * copied to the clipboard for the user to paste as the caption while the raw award photos ride
 * along as the shared images.
 */
internal fun sharePatchAwards(context: Context, groups: List<PatchEventGroup>, text: String) {
    if (groups.isEmpty()) return

    // Copy the text so the user can paste it as the Facebook caption (Facebook drops the
    // intent's pre-filled text for image shares).
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Patch awards", text))

    // FileProvider can't grant a URI for a photo stored on a secondary volume (e.g. the SD card,
    // if the storage setting is on) - its <external-files-path> only covers the primary volume.
    // Copy such a photo into internal storage first; clearShareCache keeps that scratch folder
    // from growing unbounded across repeated shares.
    clearShareCache(context)
    val photoUris: List<Uri> = groups
        .mapNotNull { it.photoPath?.takeIf { path -> path.isNotBlank() } }
        .distinct()
        .map { patchPhotoUriFor(context, preparePhotoForSharing(context, it)) }

    val builder = ShareCompat.IntentBuilder(context)
        .setChooserTitle("Share patch awards")
        .setText(text)

    if (photoUris.isNotEmpty()) {
        builder.setType("image/*")
        photoUris.forEach { builder.addStream(it) }
    } else {
        builder.setType("text/plain")
    }

    builder.startChooser()

    Toast.makeText(
        context,
        "Summary copied — paste it as your Facebook caption.",
        Toast.LENGTH_LONG
    ).show()
}

internal fun buildShareSummary(
    groups: List<PatchEventGroup>,
    teams: List<TeamWithMembers>,
    repeatLineIds: Set<Long>
): String {
    val lines = groups.map { group ->
        // Grouped by name (preserving first-seen order) rather than deduped away, so a patch
        // awarded more than once in this same entry still shows up - collapsed onto one line
        // with a "×N" suffix instead of one line per identical patch. When there's more than one
        // line, "repeat" gets a count of its own - how many of those N lines are themselves a
        // repeat (i.e. not the player's first-ever award of this patch this session) - since a
        // duplicate-within-one-award and a genuine repeat are different facts that can both be
        // true at once: e.g. 3 of the same patch in one award, where the first is this player's
        // first award of it this session, reads "×3 (repeat ×2)". A lone (non-duplicated) line
        // just gets a plain "(repeat)" tag as before, matching the in-list Repeat badge. "(raffle)"
        // is unqualified either way - it flags the patch, not a per-line count.
        val patches = group.lines
            .groupBy { it.patchName }
            .entries
            .joinToString(", ") { (name, linesForName) ->
                val count = linesForName.size
                val repeatCount = linesForName.count { it.lineId in repeatLineIds }
                val isRaffle = linesForName.any { it.status == PatchLineStatus.RAFFLE }
                val tags = buildList {
                    if (repeatCount > 0) add(if (count > 1) "repeat ×$repeatCount" else "repeat")
                    if (isRaffle) add("raffle")
                }
                val tagSuffix = if (tags.isEmpty()) "" else " (${tags.joinToString(", ")})"
                val countSuffix = if (count > 1) " ×$count" else ""
                "$name$countSuffix$tagSuffix"
            }
        // The team the player is on for this award's division (one team per player per division),
        // added in parentheses after their name. Awards with no division / no matching team just
        // show the name.
        val teamName = teams.firstOrNull { tw ->
            tw.team.division == group.division && tw.members.any { it.id == group.playerId }
        }?.team?.name
        val who = if (teamName.isNullOrBlank()) group.playerName else "${group.playerName} ($teamName)"
        "$who — $patches"
    }
    return buildString {
        append("Patch awards! 🎉")
        append("\n\n")
        append(lines.joinToString("\n"))
    }
}
