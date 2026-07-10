package com.prolocity.patchtracker.ui.components

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

fun createPatchPhotoFile(context: Context): File {
    val dir = File(context.filesDir, "patch_photos").apply { mkdirs() }
    return File(dir, "patch_${UUID.randomUUID()}.jpg")
}

fun patchPhotoUriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

// Copy a picked (gallery/photo-picker) image into app-private patch_photos/ storage and return
// the new file. The picker hands back a transient content URI we don't own; the DB stores an
// absolute file path, so we must copy the bytes into a file the app controls. Returns null if
// the source can't be read.
fun copyUriToPatchPhotoFile(context: Context, uri: Uri): File? =
    runCatching {
        val file = createPatchPhotoFile(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file
    }.getOrNull()
