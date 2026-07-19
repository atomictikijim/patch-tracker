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
