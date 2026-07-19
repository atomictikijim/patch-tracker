package com.prolocity.patchtracker.ui.patches

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ShareCompat
import com.prolocity.patchtracker.data.PatchLineStatus
import com.prolocity.patchtracker.data.TeamWithMembers
import com.prolocity.patchtracker.ui.components.patchPhotoUriFor
import java.io.File

/**
 * Shares the selected patch awards out to another app (typically Facebook).
 *
 * Facebook removed the Groups API in April 2024, so there's no way to post to a group
 * programmatically — the only route is the system share sheet, where the user picks the group
 * and posts manually. Facebook also strips pre-filled captions from image shares, so the
 * player/patch summary is copied to the clipboard for the user to paste as the caption while the
 * raw award photos ride along as the shared images.
 */
internal fun sharePatchAwards(
    context: Context,
    groups: List<PatchEventGroup>,
    teams: List<TeamWithMembers>,
    repeatLineIds: Set<Long>
) {
    if (groups.isEmpty()) return

    val summary = buildSummary(groups, teams, repeatLineIds)

    // Copy the summary so the user can paste it as the Facebook caption (Facebook drops the
    // intent's pre-filled text for image shares).
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Patch awards", summary))

    val photoUris: List<Uri> = groups
        .mapNotNull { it.photoPath?.takeIf { path -> path.isNotBlank() } }
        .distinct()
        .map { patchPhotoUriFor(context, File(it)) }

    val builder = ShareCompat.IntentBuilder(context)
        .setChooserTitle("Share patch awards")
        .setText(summary)

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

private fun buildSummary(
    groups: List<PatchEventGroup>,
    teams: List<TeamWithMembers>,
    repeatLineIds: Set<Long>
): String {
    val lines = groups.map { group ->
        // Grouped by name (preserving first-seen order) rather than deduped away, so a patch
        // awarded more than once in this same entry still shows up - just collapsed onto one
        // line with a "×N" suffix instead of one line per identical patch. Tags come from the
        // group's first (earliest) line: "(repeat)" if the player earned this patch before this
        // entry - either an earlier line in this same award or an earlier award entirely (matches
        // the in-list Repeat badge, computed from repeatLineIds) - and/or "(raffle)" if they
        // opted for the Mini Mania raffle instead of taking the patch (matches the in-list purple
        // Raffle status badge). A same-entry duplicate's own line is never itself the "first" one
        // seen for its key, so it never mints an artificial repeat tag just from being duplicated.
        val patches = group.lines
            .groupBy { it.patchName }
            .entries
            .joinToString(", ") { (name, linesForName) ->
                val first = linesForName.first()
                val tags = buildList {
                    if (first.lineId in repeatLineIds) add("repeat")
                    if (first.status == PatchLineStatus.RAFFLE) add("raffle")
                }
                val label = if (tags.isEmpty()) name else "$name (${tags.joinToString(", ")})"
                if (linesForName.size > 1) "$label ×${linesForName.size}" else label
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
