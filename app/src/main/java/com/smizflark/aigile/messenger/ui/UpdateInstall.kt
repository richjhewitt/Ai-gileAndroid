package com.smizflark.aigile.messenger.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.smizflark.aigile.messenger.data.Release
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/* ---------- Install an update (AII-314): a board build comes over the board link, a GitHub one opens in the browser ---------- */

/** Downloads a board build into the cache and opens Android's installer; any failure falls back to the GitHub release page. */
suspend fun installUpdate(context: Context, repo: Repo, r: Release, progress: (Int) -> Unit = {}) {
    if (!r.board) return view(context, r.url)
    val file = repo.boardSource?.let { b ->
        runCatching {
            val bytes = Update.download(b, r, progress)
            check(Update.matches(bytes, r.sha256)) { "the download doesn't match the board's fingerprint" }   // AII-315
            withContext(Dispatchers.IO) {
                File(context.cacheDir, "files").apply { mkdirs() }.resolve("update.apk").apply { writeBytes(bytes) }
            }
        }.getOrNull()
    }
    if (file == null || file.length() == 0L) return view(context, Update.PAGE)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".pictures", file)
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(i) }.onFailure { view(context, Update.PAGE) }
}

private fun view(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
