package com.prolocity.patchtracker.ui.patches

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ShareCompat
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
internal fun sharePatchAwards(context: Context, groups: List<PatchEventGroup>) {
    if (groups.isEmpty()) return

    val summary = buildSummary(groups)

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

private fun buildSummary(groups: List<PatchEventGroup>): String {
    val lines = groups.map { group ->
        val patches = group.lines.map { it.patchName }.distinct().joinToString(", ")
        "${group.playerName} (#${group.playerNumber}) — $patches"
    }
    return buildString {
        append("Patch awards! 🎉")
        append("\n\n")
        append(lines.joinToString("\n"))
    }
}
