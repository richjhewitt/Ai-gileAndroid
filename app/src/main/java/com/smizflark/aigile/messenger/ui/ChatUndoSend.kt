package com.smizflark.aigile.messenger.ui

import android.content.res.Resources
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Held
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.UndoSend
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/* ---------- undo send hold ---------- */

/** Undo send (AII-202): leaving the chat sends the held message at once. */
internal fun flushHeld(repo: Repo, ref: ChatRef, h: Held) {
    val t = h.body.trim()
    if (t.isNotEmpty() || h.files.isNotEmpty()) {
        val p = repo.queue(ref, t, h.to, h.files)
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch { repo.deliver(ref, p) }
    }
}

/** Shows the Undo bar for the held message; null when the wait ran out, else what was tapped (AII-202, seconds from Settings AII-233). */
internal suspend fun showUndoBar(snack: SnackbarHostState, res: Resources): SnackbarResult? {
    snack.currentSnackbarData?.dismiss()
    return withTimeoutOrNull(UndoSend.waitMs(Appearance.undoSeconds)) {
        snack.showSnackbar(res.getString(R.string.message_sent_undo, UndoSend.seconds(Appearance.undoSeconds)), res.getString(R.string.undo), duration = SnackbarDuration.Indefinite)
    }
}
