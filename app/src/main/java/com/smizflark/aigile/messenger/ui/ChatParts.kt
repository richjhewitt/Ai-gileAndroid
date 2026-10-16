package com.smizflark.aigile.messenger.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.LEAD_CHAT
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Forward
import com.smizflark.aigile.messenger.data.Quote
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.SendState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import android.os.Build
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.snapshotFlow
import com.smizflark.aigile.messenger.data.ScrollPlace
import com.smizflark.aigile.messenger.data.ScrollPlaces
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalView

internal sealed interface Row {
    val id: String
    data class Day(override val id: String, val label: String) : Row
    data class Card(override val id: String, val card: String, val title: String, val waiting: Boolean) : Row
    data class Msg(override val id: String, val b: Bubble, val pending: Pending? = null, val showOptions: Boolean = false) : Row
    /** AII-100: the "New messages" line above the first message you hadn't seen. */
    data object New : Row { override val id = "new" }
}


/** AII-339: the pinned bar under the top bar: the shown pin's first line, "1 of N" when several; tap jumps to it (or shows the next), X unpins it. */
@Composable
internal fun PinnedBar(text: String, index: Int, count: Int, onTap: () -> Unit, onUnpin: () -> Unit) {
    val on = MaterialTheme.colorScheme.onSecondaryContainer
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).clickable(onClick = onTap).padding(start = 14.dp, end = 4.dp).testTag("pinned-bar"),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PushPin, null, Modifier.size(16.dp), tint = on)
        Text(text, Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = on)
        if (count > 1) Text(stringResource(R.string.pinned_of, index + 1, count), style = MaterialTheme.typography.labelSmall, color = on, modifier = Modifier.testTag("pinned-count"))
        IconButton(onClick = onUnpin, Modifier.size(36.dp).testTag("pinned-x")) { Icon(Icons.Default.Close, stringResource(R.string.unpin_message), Modifier.size(18.dp), tint = on) }
    }
}

/** AII-78: the find bar: a text field, "2 of 5", previous / next match and close. */
@Composable
internal fun FindBar(query: String, onQuery: (String) -> Unit, count: Int, current: Int, onUp: () -> Unit, onDown: () -> Unit, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).testTag("find-bar"), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(query, onQuery, Modifier.weight(1f).testTag("find-field"), singleLine = true,
            placeholder = { Text(stringResource(R.string.find_hint)) })
        Text(
            if (query.trim().length < Find.MIN_LETTERS) "" else if (count == 0) stringResource(R.string.find_none) else stringResource(R.string.find_count, current + 1, count),
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp).testTag("find-count"),
        )
        IconButton(onClick = onUp, enabled = count > 0) { Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.find_previous)) }
        IconButton(onClick = onDown, enabled = count > 0) { Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.find_next)) }
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.find_close)) }
    }
}

/** Scrolled up: a small arrow back to the newest message, or "N new" when replies came in meanwhile. */
@Composable
internal fun BoxScope.JumpToNewest(show: Boolean, unread: Int, onClick: () -> Unit) {
    if (!show) return
    if (unread > 0) ExtendedFloatingActionButton(
        onClick = onClick, icon = { Icon(Icons.Default.ArrowDownward, null) }, text = { Text(stringResource(R.string.jump_unread, unread)) },
        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).testTag("jump"),
    ) else SmallFloatingActionButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).testTag("jump")) {
        Icon(Icons.Default.ArrowDownward, stringResource(R.string.jump_newest))
    }
}

/** At the newest message: a small button to the next unread chat (AII-204). */
@Composable
internal fun BoxScope.NextUnreadButton(name: String?, onClick: () -> Unit) {
    if (name == null) return
    AssistChip(
        onClick = onClick, label = { Text(stringResource(R.string.next_unread, name), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).widthIn(max = 320.dp).testTag("next-unread"),
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),   // solid, so text never shows through
    )
}

/**
 * For a newest-first list of rows: the day each row sits under (AII-117), from the nearest Day row above it.
 * Null for a row that is itself a Day row (it already shows its day) or has none above.
 */
internal fun daysAbove(rows: List<Row>): List<String?> {
    val out = arrayOfNulls<String>(rows.size)
    var day: String? = null
    for (k in rows.indices.reversed()) {
        val r = rows[k]
        if (r is Row.Day) { day = r.label; out[k] = null } else out[k] = day
    }
    return out.toList()
}

/** The floating day pill at the top of a chat while it scrolls (AII-117). */
@Composable
internal fun BoxScope.DayPill(label: String?, onClick: () -> Unit = {}) {
    androidx.compose.animation.AnimatedVisibility(label != null, Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
        var last by remember { mutableStateOf("") }
        if (label != null) last = label
        val goTo = stringResource(R.string.go_to_date)   // AII-256
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 2.dp,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClickLabel = goTo, onClick = onClick).testTag("day-pill")) {
            Text(last, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CardHeader(r: Row.Card, chosen: Boolean, onOpen: (() -> Unit)?, onClick: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Box {
        Surface(
            shape = shape,
            color = if (r.waiting) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(shape).combinedClickable(onClick = onClick, onLongClickLabel = stringResource(R.string.options),
                onLongClick = if (r.card == LEAD_CHAT) null else ({ menu = true })),
        ) {
            Text(
                (if (r.waiting) "⏳ " else "") + (if (r.card == LEAD_CHAT) r.title else "${r.card}  ${r.title}") + if (chosen) "  ✓" else "",
                style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 260.dp),
            )
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.copy_card_id)) }, modifier = Modifier.testTag("copy-card-id"), onClick = {
                menu = false
                clipboard.setText(AnnotatedString(r.card))
                // Android 13+ shows its own "copied" note.
                if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
            })
            if (onOpen != null) DropdownMenuItem(text = { Text(stringResource(R.string.open_card_menu)) },
                modifier = Modifier.testTag("open-card-menu"), onClick = { menu = false; onOpen() })
        }
        }
        if (onOpen != null) IconButton(onClick = onOpen, modifier = Modifier.size(40.dp).testTag("open:${r.card}")) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.open_card, r.card), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}


/** A quoted line (AII-60): a coloured bar, then the quote, a little quieter than the message. */
@Composable
internal fun QuoteBar(text: AnnotatedString, fg: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Row(modifier.padding(bottom = 4.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.75f), maxLines = 3,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Pick the chat to forward to (AII-71): the chats list with a search box. */
@Composable
internal fun ForwardDialog(repo: Repo, onDismiss: () -> Unit, onPick: (ChatRef) -> Unit) {
    var chats by remember { mutableStateOf(repo.store.cachedChats() ?: emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { runCatching { chats = repo.chats() } }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.forward_to)) },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("forward-search"),
                    placeholder = { Text(stringResource(R.string.forward_search)) })
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(Forward.pickable(chats, query), key = { it.key }) { c ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 8.dp).testTag("fwd:${c.key}"), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(c, repo, 36.dp)
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(c.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** AII-71: forwards [body] to the chosen chat by the normal send path: queued first, so it waits and retries if the phone is offline. */
@Composable
internal fun ForwardHost(scope: kotlinx.coroutines.CoroutineScope, repo: Repo, ref: ChatRef, body: String, onDismiss: () -> Unit, refreshPending: () -> Unit, reload: suspend () -> Boolean) {
    val ctx = LocalContext.current
    ForwardDialog(repo, onDismiss = onDismiss) { to ->
        onDismiss()
        val p = repo.queue(to, Forward.text(body), null)
        if (to.key == ref.key) refreshPending()
        scope.launch {
            val r = repo.deliver(to, p)
            if (to.key == ref.key) { reload(); refreshPending() }
            Toast.makeText(ctx, ctx.getString(if (r.isSuccess) R.string.forwarded_to else R.string.forward_queued, to.name), Toast.LENGTH_SHORT).show()
        }
    }
}
