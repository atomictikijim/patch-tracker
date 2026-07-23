package com.prolocity.patchtracker.ui.components

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.prolocity.patchtracker.PatchTrackerApplication
import java.io.File
import java.util.UUID

// Patch award event photos and patch type photos are kept in separate app-private folders (rather
// than the one flat folder used before) so an orphan-cleanup scan of one can never accidentally
// reach into the other - see cleanUpOrphanedAwardPhotos.
private const val AWARD_PHOTOS_DIR_NAME = "patch_award_photos"
private const val TYPE_PHOTOS_DIR_NAME = "patch_type_photos"
private const val SHARE_CACHE_DIR_NAME = "patch_photos_share_cache"

// Any storage volume beyond the primary one that's genuinely removable media (an SD card) and
// currently mounted - as opposed to a second internal partition some OEMs also expose here.
fun availableSdCardDir(context: Context): File? =
    context.getExternalFilesDirs(null).drop(1).firstOrNull { dir ->
        dir != null && Environment.isExternalStorageRemovable(dir) &&
            Environment.getExternalStorageState(dir) == Environment.MEDIA_MOUNTED
    }

// The currently-active root for NEW award photos: the SD card if the user has opted in and one is
// present, otherwise internal app storage. Falls back to internal silently if the setting is on but
// no card is currently available (e.g. it was removed after being enabled) - existing photos already
// on a now-missing card simply fail to load rather than crashing anything.
private fun patchAwardPhotosRoot(context: Context): File {
    val settings = (context.applicationContext as PatchTrackerApplication).settings
    val sdDir = if (settings.useSdCardForAwardPhotos) availableSdCardDir(context) else null
    return sdDir ?: context.filesDir
}

fun patchAwardPhotosDir(context: Context): File =
    File(patchAwardPhotosRoot(context), AWARD_PHOTOS_DIR_NAME).apply { mkdirs() }

fun patchTypePhotosDir(context: Context): File =
    File(context.filesDir, TYPE_PHOTOS_DIR_NAME).apply { mkdirs() }

fun createPatchAwardPhotoFile(context: Context): File =
    File(patchAwardPhotosDir(context), "award_${UUID.randomUUID()}.jpg")

fun createPatchTypePhotoFile(context: Context): File =
    File(patchTypePhotosDir(context), "type_${UUID.randomUUID()}.jpg")

fun patchPhotoUriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

// Copy a picked (gallery/photo-picker) image into an app-private folder and return the new file.
// The picker hands back a transient content URI we don't own; the DB stores an absolute file path,
// so we must copy the bytes into a file the app controls. Returns null if the source can't be read.
fun copyUriToPatchPhotoFile(context: Context, uri: Uri, targetDir: File): File? =
    runCatching {
        targetDir.mkdirs()
        val file = File(targetDir, "copy_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file
    }.getOrNull()

// Every folder that might currently hold award photos: internal storage always, plus the SD card's
// copy of the folder if a card is presently mounted - regardless of whether the SD setting is
// presently on, since a photo taken while it *was* on keeps its path even after the setting is
// later toggled off.
private fun allPatchAwardPhotoDirs(context: Context): List<File> {
    val dirs = mutableListOf(File(context.filesDir, AWARD_PHOTOS_DIR_NAME))
    availableSdCardDir(context)?.let { dirs.add(File(it, AWARD_PHOTOS_DIR_NAME)) }
    return dirs
}

// Deletes every file under the award-photos folder(s) that isn't in [referencedPaths] (the current
// set of patch_award_events.photoPath values). Run on session export/finalize: no code path that
// abandons a photo (cancelled capture, retake, "Remove Photo", or finalize's own event deletion)
// ever deletes the underlying file - see PatchEditScreen's photo handling and
// PatchAwardDao.finalizeCarryingOwed.
fun cleanUpOrphanedAwardPhotos(context: Context, referencedPaths: Set<String>) {
    allPatchAwardPhotoDirs(context).forEach { dir ->
        dir.listFiles()?.forEach { file ->
            if (file.absolutePath !in referencedPaths) {
                runCatching { file.delete() }
            }
        }
    }
}

// FileProvider's <external-files-path> only grants URIs under the PRIMARY external storage volume
// (Context.getExternalFilesDir(null)), never a secondary volume like an SD card - so a photo actually
// stored on the card can't be FileProvider-granted directly. Copy it into internal storage first;
// call clearShareCache before a batch of these so the scratch folder doesn't grow unbounded.
fun preparePhotoForSharing(context: Context, path: String): File {
    val file = File(path)
    if (file.absolutePath.startsWith(context.filesDir.absolutePath)) return file
    val cacheDir = File(context.filesDir, SHARE_CACHE_DIR_NAME).apply { mkdirs() }
    val copy = File(cacheDir, file.name)
    file.copyTo(copy, overwrite = true)
    return copy
}

fun clearShareCache(context: Context) {
    File(context.filesDir, SHARE_CACHE_DIR_NAME).listFiles()?.forEach { runCatching { it.delete() } }
}
