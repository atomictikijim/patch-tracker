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
