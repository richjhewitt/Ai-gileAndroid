package com.smizflark.aigile.messenger.ui

import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Closed
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.nextUnread

/* ---------- pinch to zoom text ---------- */

/** AII-259: pinch the chat to change the text size; only reacts while 2+ fingers are down and never consumes. */
@Composable
internal fun Modifier.pinchTextSize(repo: Repo): Modifier {
    val pinchCtx = LocalContext.current
    return pointerInput(Unit) {
        // AII-259: only reacts while 2+ fingers are down; never consumes, so scroll, swipe, long-press and double-tap are untouched
        awaitEachGesture {
            var start = 0f
            var done = false
            do {
                val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                val down = ev.changes.filter { it.pressed }
                if (down.size >= 2 && !done) {
                    val d = (down[0].position - down[1].position).getDistance()
                    if (start == 0f) start = d
                    else if (start > 0f) {
                        val cur = Appearance.textSize
                        val next = nextTextSize(cur, d / start)
                        if (next != cur) {
                            done = true
                            Appearance.textSize = next; repo.store.textSize = next.id
                            Toast.makeText(pinchCtx, pinchCtx.getString(next.res), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } while (ev.changes.any { it.pressed })
        }
    }
}

/* ---------- next unread ---------- */

/** AII-204: the next unread chat in chats-list order, worked out as the chats list does it. */
internal fun nextUnreadChat(repo: Repo, key: String): ChatRef? {
    val all = repo.store.cachedChats().orEmpty()
    val seenAll = repo.store.seen()
    val counts = all.associate { it.key to News.unread(it, seenAll[it.key], repo.store.cachedConversation(it.key)) }
    val shut = repo.store.closed().let { cl -> all.filter { Closed.hidden(it, cl[it.key]) }.map { it.key }.toSet() }
    return nextUnread(all, repo.store.pinned, counts, repo.store.muted(), shut, key)
}
