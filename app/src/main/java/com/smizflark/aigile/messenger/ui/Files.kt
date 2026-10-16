package com.smizflark.aigile.messenger.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.fileMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

private val client by lazy { OkHttpClient() }

/** AII-197: a file in a chat as a "📎 name" line; tap to open it in another app. */
@Composable
fun FileLine(p: Pic, repo: Repo, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val label = p.name.ifEmpty { stringResource(R.string.file) }
    Text("📎 $label", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.clickable { scope.launch { openFile(ctx, p, repo) } }.testTag("file"))
}

/** Downloads (board files with this phone's key) or copies the file into the cache, then opens it with ACTION_VIEW. */
suspend fun openFile(ctx: Context, p: Pic, repo: Repo) {
    Toast.makeText(ctx, ctx.getString(R.string.file_downloading), Toast.LENGTH_SHORT).show()
    val file = withContext(Dispatchers.IO) { runCatching { fetch(ctx, p, repo) }.getOrNull() }
    if (file == null) { Toast.makeText(ctx, ctx.getString(R.string.file_not_opened), Toast.LENGTH_SHORT).show(); return }
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(ctx, ctx.packageName + ".pictures", file), fileMime(p))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { ctx.startActivity(view) }.isFailure) Toast.makeText(ctx, ctx.getString(R.string.file_no_app), Toast.LENGTH_LONG).show()
}

internal fun fetch(ctx: Context, p: Pic, repo: Repo, dir: File = File(ctx.cacheDir, "files").apply { mkdirs(); listFiles()?.forEach { it.delete() } }, prefix: String = ""): File? {   // default: only the last file is kept
    val safe = p.name.ifEmpty { "file" }.replace(Regex("[\\/:*?\"<>|]"), "_")
    val out = File(dir, prefix + safe)
    if (p.local) {
        (ctx.contentResolver.openInputStream(Uri.parse(p.path)) ?: return null).use { i -> out.outputStream().use { i.copyTo(it) } }
        return out
    }
    val board = repo.boardSource
    val req = Request.Builder()
    if (p.web) req.url(p.path)   // another host: never the board key
    else if (board != null && p.path.startsWith("/")) req.url(board.url(p.path)).header("Authorization", "Bearer ${repo.store.key}")   // only board paths get the key
    else return null
    client.newCall(req.build()).execute().use { r ->
        if (!r.isSuccessful) return null
        r.body?.byteStream()?.use { i -> out.outputStream().use { i.copyTo(it) } } ?: return null
    }
    return out
}
