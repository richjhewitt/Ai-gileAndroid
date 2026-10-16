package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.smizflark.aigile.messenger.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Save (null before Android 10) and Share for a picture, with their notes; used full screen and on a held picture (AII-104). */
internal class PictureActions(val save: ((ImageRequest, String) -> Unit)?, val share: (ImageRequest, String) -> Unit)

@Composable
internal fun rememberPictureActions(): PictureActions {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = stringResource(R.string.picture_saved)
    val notSaved = stringResource(R.string.picture_not_saved)
    val title = stringResource(R.string.share_picture)
    val notShared = stringResource(R.string.picture_not_shared)
    return remember(ctx, scope, saved, notSaved, title, notShared) {
        PictureActions(
            // AII-95: Android 10 and up save without asking for storage access.
            save = if (Build.VERSION.SDK_INT >= 29) { req, name ->
                scope.launch { Toast.makeText(ctx, if (savePicture(ctx, req, name)) saved else notSaved, Toast.LENGTH_SHORT).show() }
            } else null,
            // AII-96: Android's share menu
            share = { req, name -> scope.launch { if (!sharePicture(ctx, req, name, title)) Toast.makeText(ctx, notShared, Toast.LENGTH_SHORT).show() } },
        )
    }
}

/** AII-95: saves the picture into the phone's Pictures/AI-gile folder. */
@RequiresApi(29)
private suspend fun savePicture(ctx: Context, req: ImageRequest, name: String): Boolean = withContext(Dispatchers.IO) {
    val bmp = loadBitmap(ctx, req) ?: return@withContext false
    val f = PicFormat(name)
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, f.fileName)
        put(MediaStore.Images.Media.MIME_TYPE, f.mime)
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI-gile")
    }
    val r = ctx.contentResolver
    val uri = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
    val ok = runCatching { r.openOutputStream(uri)!!.use { f.write(bmp, it) } }.getOrDefault(false)
    if (!ok) r.delete(uri, null, null)
    ok
}

/** AII-96: hands the picture to Android's share menu, through a copy in the app's cache (only the last one is kept). */
private suspend fun sharePicture(ctx: Context, req: ImageRequest, name: String, title: String): Boolean {
    val f = PicFormat(name)
    val file = withContext(Dispatchers.IO) {
        val bmp = loadBitmap(ctx, req) ?: return@withContext null
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        File(dir, f.fileName).takeIf { out -> runCatching { out.outputStream().use { f.write(bmp, it) } }.getOrDefault(false) }
    } ?: return false
    val send = Intent(Intent.ACTION_SEND).setType(f.mime)
        .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ctx, ctx.packageName + ".pictures", file))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return runCatching { ctx.startActivity(Intent.createChooser(send, title)) }.isSuccess
}

private suspend fun loadBitmap(ctx: Context, req: ImageRequest): Bitmap? =
    (ctx.imageLoader.execute(req.newBuilder().allowHardware(false).build()) as? SuccessResult)?.drawable?.toBitmap()

/** JPEG unless it was a PNG. */
private class PicFormat(name: String) {
    private val png = name.endsWith(".png", ignoreCase = true)
    val fileName = name.substringBeforeLast('.').ifEmpty { "picture" } + if (png) ".png" else ".jpg"
    val mime = if (png) "image/png" else "image/jpeg"
    fun write(bmp: Bitmap, out: java.io.OutputStream) = bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, out)
}
