package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.data.Repo
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Mute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ---------- chat top-bar menu actions ---------- */

/** Add to home screen; a launcher that cannot pin gets a note instead. */
internal fun addChatToHome(ctx: Context, ref: ChatRef) {
    if (!com.smizflark.aigile.messenger.data.HomeShortcut.pin(ctx, ref.key, ref.name)) Toast.makeText(ctx, ctx.getString(R.string.add_to_home_unsupported), Toast.LENGTH_SHORT).show()
}

/** Silent on or off for this chat; returns whether it is silent now. */
internal fun toggleChatSilent(repo: Repo, ref: ChatRef): Boolean {
    repo.store.silent = com.smizflark.aigile.messenger.data.Silent.toggle(repo.store.silent, ref.key)
    return ref.key in repo.store.silent
}

/** Mute for a while, [f] says how long. */
internal fun muteChatFor(repo: Repo, ref: ChatRef, f: Mute.For) =
    repo.store.setMuted(ref.key, true, Mute.until(com.smizflark.aigile.messenger.data.Now.millis(), f))

/* ---------- jumps ---------- */

/**
 * Scrolls to the row [key] (the list is newest first) and outlines it for a moment through [setFlash]/[flash];
 * a row that is not loaded gets a note. AII-138 starred, AII-242 My last message.
 */
internal fun jumpToRow(key: String, items: List<Row>, list: LazyListState, scope: CoroutineScope, ctx: Context, flash: () -> String?, setFlash: (String?) -> Unit) {
    val i = items.indexOfFirst { it.id == key }
    if (i < 0) { Toast.makeText(ctx, R.string.star_not_loaded, Toast.LENGTH_SHORT).show(); return }
    scope.launch { list.animateScrollToItem(i + 1); setFlash(key); delay(1500); if (flash() == key) setFlash(null) }   // +1: the spacer at the bottom
}

/** My last message scrolls to your newest sent message, same jump as a starred one. */
internal fun jumpToMyLast(items: List<Row>, list: LazyListState, scope: CoroutineScope, ctx: Context, flash: () -> String?, setFlash: (String?) -> Unit) {
    val m = items.firstOrNull { it is Row.Msg && it.b.mine }
    if (m == null) Toast.makeText(ctx, R.string.no_my_message, Toast.LENGTH_SHORT).show() else jumpToRow(m.id, items, list, scope, ctx, flash, setFlash)
}
