package com.smizflark.aigile.messenger.ui

import android.content.res.Resources
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* ---------- picking and pasting pictures ---------- */

/** Pictures (AII-27): the system photo picker needs no permission; each pick is copied into the app at once. */
@Composable
internal fun rememberPicPicker(repo: Repo, pics: () -> List<String>, setPics: (List<String>) -> Unit, setNote: (String?) -> Unit): ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>> {
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources
    return rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICS)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            setNote(res.getQuantityString(R.plurals.adding_pictures, uris.size, uris.size))
            val kept = withContext(Dispatchers.IO) { uris.map { repo.keepPicture(it) } }
            setPics((pics() + kept.filterNotNull()).take(MAX_PICS))
            setNote(if (kept.any { it == null }) res.getString(R.string.pic_read_one) else null)
        }
    }
}

/** A copied picture (AII-56): offered as a chip above the message box; this adds it. */
internal fun pasteCopiedPicture(copied: CopiedPicture, repo: Repo, scope: CoroutineScope, res: Resources, pics: () -> List<String>, setPics: (List<String>) -> Unit, setNote: (String?) -> Unit) {
    val uri = copied.take() ?: run { setNote(res.getString(R.string.pic_read_copied)); return }
    scope.launch {
        val kept = withContext(Dispatchers.IO) { repo.keepPicture(uri) }
        if (kept == null) setNote(res.getString(R.string.pic_read_copied)) else { setPics((pics() + kept).take(MAX_PICS)); setNote(null) }
    }
}
