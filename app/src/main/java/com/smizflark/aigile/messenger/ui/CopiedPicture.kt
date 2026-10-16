package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.ClipDescription
import android.content.ClipboardManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/* ---------- a copied picture (AII-56) ---------- */

/** Is there a picture on the clipboard that hasn't been pasted yet? Reads only the clip's type and time, so Android shows no "pasted" notice. */
fun clipHasNewPicture(mimes: List<String>, stamp: Long, pastedStamp: Long) = stamp != pastedStamp && mimes.any { it.startsWith("image/") }

/** Watches the clipboard for a copied picture (a screenshot you tapped Copy on). take() reads it once and hides the offer. */
class CopiedPicture(private val cm: ClipboardManager) {
    var available by mutableStateOf(false); private set
    private var pastedStamp = -1L

    fun check() {
        val d: ClipDescription? = cm.primaryClipDescription
        available = d != null && clipHasNewPicture((0 until d.mimeTypeCount).map { d.getMimeType(it) }, d.timestamp, pastedStamp)
    }

    fun take(): Uri? {
        pastedStamp = cm.primaryClipDescription?.timestamp ?: -1L
        available = false
        val clip = cm.primaryClip ?: return null
        return (0 until clip.itemCount).firstNotNullOfOrNull { clip.getItemAt(it).uri }
    }
}

@Composable
fun rememberCopiedPicture(): CopiedPicture {
    val ctx = LocalContext.current
    val cp = remember { CopiedPicture(ctx.getSystemService(ClipboardManager::class.java)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { cp.check() }
    DisposableEffect(cp) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        val l = ClipboardManager.OnPrimaryClipChangedListener { cp.check() }
        cm.addPrimaryClipChangedListener(l)
        onDispose { cm.removePrimaryClipChangedListener(l) }
    }
    return cp
}
