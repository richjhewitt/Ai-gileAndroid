package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Mute
import com.smizflark.aigile.messenger.data.Silent
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.SwipeAction
import com.smizflark.aigile.messenger.data.SwipeChoice
import com.smizflark.aigile.messenger.data.swipeAction


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatRow(c: ChatRef, repo: Repo, fresh: Boolean, unread: Int, pinned: Boolean, muted: Boolean, reminded: Boolean, unsent: List<Pending>, draft: String?, closed: Boolean,
                    onRead: () -> Unit, onUnread: () -> Unit, onMute: (Boolean) -> Unit, onPin: () -> Unit, onClose: (Boolean) -> Unit, onAddHome: () -> Unit = {}, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    // Swipes do what Settings says (AII-216; starting: right = mark read, left = mute/unmute, AII-61). The row always springs back.
    val mutedNow by rememberUpdatedState(muted)
    val closedNow by rememberUpdatedState(closed)
    val right = Appearance.swipeRight
    val left = Appearance.swipeLeft
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // AII-240, AII-257: Remind me in 1 hour about the chat's newest cached message, from the hold menu or a swipe.
    val remindHour = {
        val newest = repo.store.cachedConversation(c.key)?.bubbles?.lastOrNull()
        if (newest != null) {
            val at = com.smizflark.aigile.messenger.work.RemindWorker.inAnHour(ctx, repo.store, c.key, c.name, newest.key, Markdown.plain(Markdown.render(newest.text, Color.Transparent, Color.Transparent)).trim())
            if (at != null) android.widget.Toast.makeText(ctx, ctx.getString(R.string.remind_set, Mute.endTime(at)), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val box = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (swipeAction(when (v) { SwipeToDismissBoxValue.StartToEnd -> true; SwipeToDismissBoxValue.EndToStart -> false; else -> null }, mutedNow, right, left)) {
            SwipeAction.READ -> onRead()
            SwipeAction.MUTE -> onMute(true)
            SwipeAction.UNMUTE -> onMute(false)
            SwipeAction.PIN -> onPin()
            SwipeAction.CLOSE -> onClose(!closedNow)
            SwipeAction.REMIND -> remindHour()
            null -> {}
        }
        false
    })
    SwipeToDismissBox(box, modifier = Modifier.testTag("swipe:${c.key}"), enableDismissFromStartToEnd = right != SwipeChoice.NOTHING,
        enableDismissFromEndToStart = left != SwipeChoice.NOTHING, backgroundContent = {
        val toRead = box.dismissDirection == SwipeToDismissBoxValue.StartToEnd
        val idle = box.dismissDirection == SwipeToDismissBoxValue.Settled
        val pick = if (toRead) right else left
        Box(Modifier.fillMaxSize().background(when {
            idle -> Color.Transparent
            toRead -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }).padding(horizontal = 24.dp), contentAlignment = if (toRead) Alignment.CenterStart else Alignment.CenterEnd) {
            if (!idle) when (pick) {
                SwipeChoice.READ -> Icon(Icons.Default.DoneAll, stringResource(R.string.mark_read))
                SwipeChoice.MUTE -> Icon(if (muted) Icons.Default.Notifications else Icons.Default.NotificationsOff, stringResource(if (muted) R.string.unmute else R.string.mute))
                SwipeChoice.PIN -> Icon(Icons.Default.PushPin, stringResource(if (pinned) R.string.unpin else R.string.pin))
                SwipeChoice.CLOSE -> Icon(Icons.Default.Close, stringResource(if (closed) R.string.reopen_chat else R.string.close_chat))
                SwipeChoice.REMIND -> Icon(Icons.Default.NotificationsActive, stringResource(R.string.remind_in_hour))
                SwipeChoice.NOTHING -> {}
            }
        }
    }) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).combinedClickable(onClickLabel = stringResource(R.string.open_chat), role = Role.Button, onClick = onClick,
            onLongClick = { menu = true }, onLongClickLabel = stringResource(R.string.options))
            .padding(horizontal = 16.dp, vertical = if (Appearance.compact) 6.dp else 12.dp).testTag("chat:${c.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            // AII-240: Remind me in 1 hour about the chat's newest message (from the cache; hidden when there is none).
            val newest = remember(menu) { if (menu) repo.store.cachedConversation(c.key)?.bubbles?.lastOrNull() else null }
            if (newest != null) DropdownMenuItem(text = { Text(stringResource(R.string.remind_in_hour)) }, leadingIcon = { Icon(Icons.Default.NotificationsActive, null) },
                onClick = {
                    menu = false
                    remindHour()
                }, modifier = Modifier.testTag("menu-remind"))
            // AII-376: pin a shortcut to this chat on the home screen.
            DropdownMenuItem(text = { Text(stringResource(R.string.add_to_home)) }, leadingIcon = { Icon(Icons.Default.Home, null) },
                onClick = { menu = false; onAddHome() }, modifier = Modifier.testTag("menu-add-home"))
            // AII-249: Copy name.
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            DropdownMenuItem(text = { Text(stringResource(R.string.copy_name)) }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = {
                    menu = false
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(c.name))
                    // Android 13+ shows its own "copied" note.
                    if (android.os.Build.VERSION.SDK_INT < 33) android.widget.Toast.makeText(ctx, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.testTag("menu-copy-name"))
            // Read chats offer Mark as unread instead (AII-119); the Lead Scrum Master goes by the board's count, so it can't.
            if (fresh || c.kind == Kind.WATCHMAN) DropdownMenuItem(text = { Text(stringResource(R.string.mark_read)) }, enabled = fresh, onClick = { menu = false; onRead() })
            else DropdownMenuItem(text = { Text(stringResource(R.string.mark_unread)) }, onClick = { menu = false; onUnread() }, modifier = Modifier.testTag("menu-unread"))
            DropdownMenuItem(text = { Text(stringResource(if (pinned) R.string.unpin else R.string.pin)) }, leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { menu = false; onPin() }, modifier = Modifier.testTag("pin"))
            // AII-103: mute here too, not only on the swipe.
            DropdownMenuItem(text = { Text(stringResource(if (muted) R.string.unmute else R.string.mute)) },
                leadingIcon = { Icon(if (muted) Icons.Default.Notifications else Icons.Default.NotificationsOff, null) },
                onClick = { menu = false; onMute(!muted) }, modifier = Modifier.testTag("menu-mute"))
            // AII-131: off the list until a newer message, on the board's Messages list too.
            DropdownMenuItem(text = { Text(stringResource(if (closed) R.string.reopen_chat else R.string.close_chat)) }, leadingIcon = { Icon(Icons.Default.Close, null) },
                onClick = { menu = false; onClose(!closed) }, modifier = Modifier.testTag("menu-close"))
        }
        Avatar(c, repo, if (Appearance.compact) 36.dp else 48.dp)   // AII-212
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Name with its pin and mute icons takes the room; the time stays at the right edge.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (fresh) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                    if (pinned) Icon(Icons.Default.PushPin, stringResource(R.string.pinned), Modifier.padding(start = 4.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (muted) Icon(Icons.Default.NotificationsOff, stringResource(R.string.muted), Modifier.padding(start = 4.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    // AII-226: a bell when a message in this chat has a reminder.
                    if (reminded) Icon(Icons.Default.NotificationsActive, stringResource(R.string.has_reminder), Modifier.padding(start = 4.dp).size(14.dp).testTag("row-reminder"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    // AII-235: silent alerts show a speaker-off icon; muted wins.
                    if (!muted && Silent.quiet(repo.store.silent, emptySet(), c.key)) Icon(Icons.AutoMirrored.Filled.VolumeOff, stringResource(R.string.silent_alerts),
                        Modifier.padding(start = 4.dp).size(16.dp).testTag("row-silent"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    // AII-165: a timed mute shows when it ends.
                    repo.store.mutedUntil()[c.key]?.takeIf { muted }?.let { end ->
                        Text(stringResource(R.string.muted_until, Mute.endTime(end)), style = MaterialTheme.typography.labelSmall, maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp).testTag("row-mute-ends"))
                    }
                }
                Text(shortTime(c.last), style = MaterialTheme.typography.labelMedium,
                    color = if (fresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val waiting = unsentLabel(unsent) ?: draftLabel(draft)
                val newDesc = stringResource(R.string.new_messages)
                Text(
                    waiting ?: previewLabel(c),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        waiting == null && c.working -> MaterialTheme.colorScheme.primary
                        waiting == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        unsent.any { it.failed && !it.offline } -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (c.waiting > 0) Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                    Text(stringResource(R.string.waiting, c.waiting), Modifier.padding(horizontal = 4.dp))
                }
                if (fresh && unread > 0) Badge(containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("unread:${c.key}")) {
                    Text(if (unread > 99) "99+" else "$unread", Modifier.padding(horizontal = 2.dp))
                } else if (fresh && c.waiting == 0) Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    .semantics { contentDescription = newDesc })
            }
        }
    }
    }
}

/** What a chat row says about messages typed on the phone that haven't reached the board (AII-52); null when none. */
fun unsentLabel(unsent: List<Pending>, now: java.time.Instant = Now.instant()): String? {
    if (unsent.isEmpty()) return null
    val failed = unsent.count { it.failed && !it.offline }   // offline ones still go by themselves (AII-55)
    // AII-148: only Send later ones left: say when the first goes.
    val held = unsent.filter { com.smizflark.aigile.messenger.work.later(it, now) }
    if (failed == 0 && held.size == unsent.size) return Words.sendsAt.format(sendTime(held.minBy { java.time.Instant.parse(it.sendAt) }.sendAt))
    return if (failed > 0) Words.unsentFailed.format(failed) else Words.unsentWaiting.format(unsent.size - held.size)
}

/** The line under a chat's name: "working…" while the team is at work (AII-112), else its newest message ("You: ..." when it is yours, AII-106), else the chat's subtitle. */
fun previewLabel(c: ChatRef): String = if (c.working) Words.working else c.preview?.let { if (c.previewMine) Words.you.format(it) else it } ?: c.subtitle

/** "Draft: ..." for a chat with a half-typed reply (AII-98), on one line; null when there is none. */
fun draftLabel(draft: String?): String? =
    draft?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }?.let { Words.draft.format(it) }
