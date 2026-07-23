package com.prolocity.patchtracker.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

// Full-screen crop/rotate editor for a patch award photo, opened from PhotoViewerDialog's Edit
// button. Hand-rolled in Compose rather than a third-party cropping library - see the crop
// interaction notes on CropOverlay below for the coordinate-mapping approach.
@Composable
fun PhotoEditorDialog(
    photoPath: String,
    onCancel: () -> Unit,
    onSave: (newFilePath: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var workingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    LaunchedEffect(photoPath) {
        workingBitmap = withContext(Dispatchers.IO) { loadOrientedBitmap(photoPath) }
    }

    fun rotate(degrees: Float) {
        val current = workingBitmap ?: return
        scope.launch {
            workingBitmap = withContext(Dispatchers.Default) { rotateBitmap(current, degrees) }
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val bitmap = workingBitmap
            if (bitmap == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            } else {
                val containerWidthPx = with(density) { maxWidth.toPx() }
                val containerHeightPx = with(density) { maxHeight.toPx() }
                val fitRect = remember(bitmap, containerWidthPx, containerHeightPx) {
                    computeFitRect(containerWidthPx, containerHeightPx, bitmap.width, bitmap.height)
                }
                var cropRect by remember(bitmap) { mutableStateOf(fitRect) }
                val minSizePx = with(density) { 32.dp.toPx() }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                CropOverlay(
                    fitRect = fitRect,
                    cropRect = cropRect,
                    minSizePx = minSizePx,
                    onCropRectChange = { cropRect = it }
                )

                fun saveEdit(exportToGallery: Boolean) {
                    isBusy = true
                    scope.launch {
                        val bitmapRect = withContext(Dispatchers.Default) {
                            mapScreenRectToBitmapRect(cropRect, fitRect, bitmap.width, bitmap.height)
                        }
                        val finalBitmap = withContext(Dispatchers.Default) { cropBitmap(bitmap, bitmapRect) }
                        if (exportToGallery) {
                            val ok = withContext(Dispatchers.IO) { saveBitmapToGallery(context, finalBitmap) }
                            Toast.makeText(
                                context,
                                if (ok) "Saved to Photos" else "Couldn't save photo",
                                Toast.LENGTH_SHORT
                            ).show()
                            isBusy = false
                        } else {
                            val newFile = withContext(Dispatchers.Default) {
                                createPatchAwardPhotoFile(context).also { f ->
                                    FileOutputStream(f).use { out ->
                                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                    }
                                }
                            }
                            isBusy = false
                            onSave(newFile.absolutePath)
                        }
                    }
                }

                val galleryPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        saveEdit(exportToGallery = true)
                    } else {
                        Toast.makeText(context, "Permission needed to save to Photos", Toast.LENGTH_SHORT).show()
                    }
                }

                IconButton(
                    onClick = onCancel,
                    enabled = !isBusy,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { rotate(-90f) }, enabled = !isBusy) {
                            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate left", tint = Color.White)
                        }
                        IconButton(onClick = { rotate(90f) }, enabled = !isBusy) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate right", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (canWriteGalleryDirectly(context)) {
                                    saveEdit(exportToGallery = true)
                                } else {
                                    galleryPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            },
                            enabled = !isBusy
                        ) {
                            Icon(Icons.Filled.SaveAlt, contentDescription = "Save to Photos", tint = Color.White)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        }
                        TextButton(onClick = onCancel, enabled = !isBusy) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(onClick = { saveEdit(exportToGallery = false) }, enabled = !isBusy) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

private enum class Corner { TopLeft, TopRight, BottomLeft, BottomRight }

// Draws the dimmed-outside/border overlay and the drag handles for the live crop selection.
// [fitRect] is the on-screen rect the photo is actually drawn into (ContentScale.Fit's own
// scale/center math, replicated in computeFitRect so this overlay lines up with it pixel-for-
// pixel); [cropRect] is the current selection, always a sub-rect of [fitRect].
@Composable
private fun CropOverlay(
    fitRect: Rect,
    cropRect: Rect,
    minSizePx: Float,
    onCropRectChange: (Rect) -> Unit
) {
    // Gesture callbacks below run inside a pointerInput keyed on Unit (installed once, never
    // restarted mid-drag) - rememberUpdatedState lets them read the latest values each callback
    // without needing to key on (and thereby restart on) values that change during the very
    // drag they're handling.
    val latestCropRect = rememberUpdatedState(cropRect)
    val latestFitRect = rememberUpdatedState(fitRect)
    val latestMinSize = rememberUpdatedState(minSizePx)
    val latestOnChange = rememberUpdatedState(onCropRectChange)
    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dimColor = Color.Black.copy(alpha = 0.5f)
        drawRect(dimColor, topLeft = Offset(fitRect.left, fitRect.top), size = Size(fitRect.width, cropRect.top - fitRect.top))
        drawRect(dimColor, topLeft = Offset(fitRect.left, cropRect.bottom), size = Size(fitRect.width, fitRect.bottom - cropRect.bottom))
        drawRect(dimColor, topLeft = Offset(fitRect.left, cropRect.top), size = Size(cropRect.left - fitRect.left, cropRect.height))
        drawRect(dimColor, topLeft = Offset(cropRect.right, cropRect.top), size = Size(fitRect.right - cropRect.right, cropRect.height))
        drawRect(
            color = Color.White,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Draggable body (reposition the whole selection) - below the corner handles in z-order.
    Box(
        modifier = Modifier
            .offset { IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt()) }
            .size(
                width = with(density) { cropRect.width.toDp() },
                height = with(density) { cropRect.height.toDp() }
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    latestOnChange.value(translateRect(latestCropRect.value, dragAmount, latestFitRect.value))
                }
            }
    )

    Corner.entries.forEach { corner ->
        val point = when (corner) {
            Corner.TopLeft -> Offset(cropRect.left, cropRect.top)
            Corner.TopRight -> Offset(cropRect.right, cropRect.top)
            Corner.BottomLeft -> Offset(cropRect.left, cropRect.bottom)
            Corner.BottomRight -> Offset(cropRect.right, cropRect.bottom)
        }
        val handleSizePx = with(density) { 40.dp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (point.x - handleSizePx / 2).roundToInt(),
                        (point.y - handleSizePx / 2).roundToInt()
                    )
                }
                .size(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        latestOnChange.value(
                            resizeFromCorner(latestCropRect.value, corner, dragAmount, latestFitRect.value, latestMinSize.value)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(16.dp).background(Color.White, CircleShape))
        }
    }
}

private fun resizeFromCorner(rect: Rect, corner: Corner, dragAmount: Offset, bounds: Rect, minSize: Float): Rect {
    var left = rect.left
    var top = rect.top
    var right = rect.right
    var bottom = rect.bottom
    when (corner) {
        Corner.TopLeft -> { left += dragAmount.x; top += dragAmount.y }
        Corner.TopRight -> { right += dragAmount.x; top += dragAmount.y }
        Corner.BottomLeft -> { left += dragAmount.x; bottom += dragAmount.y }
        Corner.BottomRight -> { right += dragAmount.x; bottom += dragAmount.y }
    }
    val maxLeft = (right - minSize).coerceAtLeast(bounds.left)
    left = left.coerceIn(bounds.left, maxLeft)
    val minRight = (left + minSize).coerceAtMost(bounds.right)
    right = right.coerceIn(minRight, bounds.right)
    val maxTop = (bottom - minSize).coerceAtLeast(bounds.top)
    top = top.coerceIn(bounds.top, maxTop)
    val minBottom = (top + minSize).coerceAtMost(bounds.bottom)
    bottom = bottom.coerceIn(minBottom, bounds.bottom)
    return Rect(left, top, right, bottom)
}

private fun translateRect(rect: Rect, dragAmount: Offset, bounds: Rect): Rect {
    val maxLeft = (bounds.right - rect.width).coerceAtLeast(bounds.left)
    val newLeft = (rect.left + dragAmount.x).coerceIn(bounds.left, maxLeft)
    val maxTop = (bounds.bottom - rect.height).coerceAtLeast(bounds.top)
    val newTop = (rect.top + dragAmount.y).coerceIn(bounds.top, maxTop)
    return Rect(newLeft, newTop, newLeft + rect.width, newTop + rect.height)
}

// Replicates ContentScale.Fit's own scale/center math so this hand-drawn overlay aligns
// pixel-for-pixel with the real Image composable using the same formula.
private fun computeFitRect(containerW: Float, containerH: Float, bitmapW: Int, bitmapH: Int): Rect {
    if (bitmapW <= 0 || bitmapH <= 0) return Rect(0f, 0f, containerW, containerH)
    val scale = minOf(containerW / bitmapW, containerH / bitmapH)
    val displayW = bitmapW * scale
    val displayH = bitmapH * scale
    val offsetX = (containerW - displayW) / 2f
    val offsetY = (containerH - displayH) / 2f
    return Rect(offsetX, offsetY, offsetX + displayW, offsetY + displayH)
}

private fun mapScreenRectToBitmapRect(screenRect: Rect, fitRect: Rect, bitmapW: Int, bitmapH: Int): IntRect {
    val scale = if (fitRect.width > 0f) fitRect.width / bitmapW else 1f
    val left = ((screenRect.left - fitRect.left) / scale).roundToInt().coerceIn(0, bitmapW - 1)
    val top = ((screenRect.top - fitRect.top) / scale).roundToInt().coerceIn(0, bitmapH - 1)
    val right = ((screenRect.right - fitRect.left) / scale).roundToInt().coerceIn(left + 1, bitmapW)
    val bottom = ((screenRect.bottom - fitRect.top) / scale).roundToInt().coerceIn(top + 1, bitmapH)
    return IntRect(left, top, right, bottom)
}

private fun cropBitmap(bitmap: Bitmap, rect: IntRect): Bitmap {
    if (rect.left == 0 && rect.top == 0 && rect.right == bitmap.width && rect.bottom == bitmap.height) {
        return bitmap
    }
    return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width, rect.height)
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Camera-captured JPEGs commonly store orientation as EXIF metadata rather than rotating pixels;
// Coil already respects that tag when rendering the thumbnail/viewer, so the editor's decoded
// bitmap must be pre-rotated to match before any manual rotate/crop, or the two would disagree.
// Flip variants (mirrored orientations) are out of scope - rare from stock camera/gallery JPEGs,
// and handling them would add real complexity for negligible benefit here.
private fun exifRotationDegrees(path: String): Int {
    val exif = runCatching { ExifInterface(path) }.getOrNull() ?: return 0
    return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

private const val MAX_EDITOR_DIMENSION = 2048

private fun computeSampleSize(rawWidth: Int, rawHeight: Int, maxDimension: Int): Int {
    var sampleSize = 1
    var w = rawWidth
    var h = rawHeight
    while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
        w /= 2
        h /= 2
        sampleSize *= 2
    }
    return sampleSize
}

// Downsampled decode (camera JPEGs can be 12-48MP; a hand-rolled editor holding that at full
// resolution risks OOM/jank on a minSdk-26-era device) plus EXIF pre-rotation.
private fun loadOrientedBitmap(path: String): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, boundsOptions)
    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = computeSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, MAX_EDITOR_DIMENSION)
    }
    val decoded = BitmapFactory.decodeFile(path, options) ?: return null
    val degrees = exifRotationDegrees(path)
    return if (degrees != 0) rotateBitmap(decoded, degrees.toFloat()) else decoded
}

private fun canWriteGalleryDirectly(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

// Exports the given (already rotated/cropped) bitmap to the device's shared Pictures gallery,
// independent of whether the in-app edit is also kept.
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean = runCatching {
    val filename = "PatchTracker_${UUID.randomUUID()}.jpg"
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PatchTracker")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
        val wrote = resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        } ?: false
        if (!wrote) return@runCatching false
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PatchTracker")
            .apply { mkdirs() }
        val file = File(dir, filename)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
    }
    true
}.getOrDefault(false)
