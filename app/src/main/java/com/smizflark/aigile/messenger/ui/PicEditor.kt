package com.smizflark.aigile.messenger.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectDragGestures
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.PicEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/* ---------- crop and rotate a picked picture (AII-385) ---------- */

private const val EDIT_SIDE = 3072

/** Decodes the picture, big ones sampled down, turned upright by its EXIF orientation. The file itself is never changed. */
internal fun loadForEdit(path: String): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(path, it) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= EDIT_SIDE) sample *= 2
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@runCatching null
    val m = Matrix()
    when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        else -> return@runCatching bmp
    }
    Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}.getOrNull()

private fun turned(b: Bitmap, quarters: Int): Bitmap {
    val q = Math.floorMod(quarters, 4)
    if (q == 0) return b
    return Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(90f * q) }, true)
}

/** Writes the edited copy next to the kept picture (JPEG 90, or PNG for a PNG) and returns its path; null if it failed. */
internal fun saveEdited(src: String, base: Bitmap, quarters: Int, crop: PicEdit.Crop): String? = runCatching {
    val rotated = turned(base, quarters)
    val px = PicEdit.toPixels(crop, rotated.width, rotated.height)
    val out = Bitmap.createBitmap(rotated, px.x, px.y, px.w, px.h)
    val from = File(src)
    val to = File(from.parentFile, PicEdit.copyName(from.name, UUID.randomUUID().toString().take(8)))
    to.outputStream().use {
        if (PicEdit.isPng(from.name)) out.compress(Bitmap.CompressFormat.PNG, 100, it) else out.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }
    to.path
}.getOrNull()

/** The full-screen editor in its own window; [onDone] gets the path of the edited copy. */
@Composable
internal fun PicEditorDialog(path: String, onCancel: () -> Unit, onDone: (String) -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        PicEditor(path, onCancel, onDone)
    }
}

@Composable
internal fun PicEditor(path: String, onCancel: () -> Unit, onDone: (String) -> Unit, preloaded: Bitmap? = null) {
    var base by remember { mutableStateOf(preloaded) }
    var failed by remember { mutableStateOf(false) }
    var quarters by remember { mutableStateOf(0) }
    var crop by remember { mutableStateOf(PicEdit.FULL) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(path) { if (base == null) { base = withContext(Dispatchers.IO) { loadForEdit(path) }; failed = base == null } }
    val shown = remember(base, quarters) { base?.let { turned(it, quarters) } }
    Surface(Modifier.fillMaxSize().testTag("pic-editor"), color = Color.Black) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (shown != null) CropArea(shown, crop) { crop = it }
                else Text(if (failed) stringResource(R.string.pic_edit_failed) else "", color = Color.White)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { quarters += 1; crop = PicEdit.rotateCw(crop) }, enabled = shown != null, modifier = Modifier.testTag("pic-rotate")) {
                    Icon(Icons.Default.RotateRight, stringResource(R.string.pic_rotate), tint = Color.White)
                }
                TextButton(onClick = { quarters = 0; crop = PicEdit.FULL }, enabled = shown != null, modifier = Modifier.testTag("pic-reset")) { Text(stringResource(R.string.reset)) }
                TextButton(onClick = onCancel, modifier = Modifier.testTag("pic-cancel")) { Text(stringResource(R.string.cancel)) }
                Button(
                    enabled = shown != null && !saving, modifier = Modifier.testTag("pic-done"),
                    onClick = {
                        val b = base ?: return@Button
                        saving = true
                        scope.launch {
                            val out = withContext(Dispatchers.IO) { saveEdited(path, b, quarters, crop) }
                            saving = false
                            if (out != null) onDone(out) else failed = true
                        }
                    },
                ) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

/** The picture, fitted, with a dimmed outside and a crop rectangle whose corners can be dragged. */
@Composable
private fun CropArea(bmp: Bitmap, crop: PicEdit.Crop, onCrop: (PicEdit.Crop) -> Unit) {
    val image = remember(bmp) { bmp.asImageBitmap() }
    val accent = MaterialTheme.colorScheme.primary
    var box by remember { mutableStateOf(IntSize.Zero) }
    // The picture fitted inside the area.
    val scale = if (box.width == 0) 1f else minOf(box.width.toFloat() / bmp.width, box.height.toFloat() / bmp.height)
    val w = bmp.width * scale; val h = bmp.height * scale
    val left = (box.width - w) / 2f; val top = (box.height - h) / 2f
    val latest = androidx.compose.runtime.rememberUpdatedState(crop)
    Canvas(
        Modifier.fillMaxSize().testTag("pic-crop")
            .onSizeChanged { box = it }
            .pointerInput(bmp, box) {
                var corner: PicEdit.Corner? = null
                detectDragGestures(
                    onDragStart = { at -> corner = PicEdit.nearest(latest.value, at.x - left, at.y - top, w, h, 48.dp.toPx()) },
                    onDragEnd = { corner = null }, onDragCancel = { corner = null },
                ) { change, _ ->
                    val k = corner ?: return@detectDragGestures
                    change.consume()
                    onCrop(PicEdit.drag(latest.value, k, (change.position.x - left) / w, (change.position.y - top) / h))
                }
            },
    ) {
        drawImage(image, dstOffset = IntOffset(left.toInt(), top.toInt()), dstSize = IntSize(w.toInt(), h.toInt()))
        val l = left + crop.l * w; val t = top + crop.t * h; val r = left + crop.r * w; val b = top + crop.b * h
        val dim = Color.Black.copy(alpha = 0.6f)
        drawRect(dim, Offset(left, top), Size(w, t - top))
        drawRect(dim, Offset(left, b), Size(w, top + h - b))
        drawRect(dim, Offset(left, t), Size(l - left, b - t))
        drawRect(dim, Offset(r, t), Size(left + w - r, b - t))
        drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(2.dp.toPx()))
        for ((x, y) in listOf(l to t, r to t, l to b, r to b)) {
            drawCircle(Color.White, 11.dp.toPx(), Offset(x, y))
            drawCircle(accent, 8.dp.toPx(), Offset(x, y))
        }
    }
}
