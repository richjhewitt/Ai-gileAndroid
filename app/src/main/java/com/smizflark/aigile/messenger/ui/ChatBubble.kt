package com.smizflark.aigile.messenger.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Schedule
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(r: Row.Msg, ref: ChatRef, repo: Repo, mark: Int, onReply: (Bubble) -> Unit, onForward: (Bubble) -> Unit, cards: Set<String>, onCard: (String) -> Unit, onOption: (String) -> Unit, onRetry: (Pending) -> Unit, onDiscard: (Pending) -> Unit, onQuote: (String) -> Unit = {}, starred: Boolean = false, onStar: (() -> Unit)? = null, onSendNow: (Pending) -> Unit = {}) {
    val b = r.b
    val cs = MaterialTheme.colorScheme
    val bg = when {
        b.unverified -> cs.errorContainer
        b.mine -> cs.primaryContainer
        else -> cs.surfaceContainerHigh
    }
    val fg = if (b.mine) cs.onPrimaryContainer else cs.onSurface
    val copy = rememberCopy()   // AII-115: tap a ``` block to copy it
    val shown = Markdown.render(b.text, cs.surfaceVariant, cs.primary, cards, onCard, copy)
    // AII-43: long-press a bubble to copy what it shows (without the ** marks).
    var menu by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }   // AII-127: pick part of a message to copy
    var showTime by remember { mutableStateOf(false) }   // AII-51: tap a bubble for its exact time
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    // AII-101: drag a message to the right to reply to it; it springs back.
    val canReply = b.state == SendState.SENT && !b.unverified
    val view = LocalView.current
    val reach = with(LocalDensity.current) { SWIPE_REPLY.toPx() }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth()) {
    if (drag.value > 0f) Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
        .graphicsLayer { alpha = replyProgress(drag.value, reach) }, tint = cs.primary)
    Column(Modifier.fillMaxWidth().offset { IntOffset(drag.value.roundToInt(), 0) }
        .draggable(rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceIn(0f, reach * 1.3f)) } },
            Orientation.Horizontal, enabled = canReply,
            onDragStopped = {
                if (replyProgress(drag.value, reach) >= 1f) { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onReply(b) }
                drag.animateTo(0f)
            })
        .padding(horizontal = 12.dp).testTag("swipe-msg"), horizontalAlignment = if (b.mine) Alignment.End else Alignment.Start) {
        val failedP = r.pending?.takeIf { b.state == SendState.FAILED }
        Row(verticalAlignment = Alignment.CenterVertically) {
        if (failedP != null) {
            var ask by remember { mutableStateOf(false) }
            val retryLabel = stringResource(R.string.retry_or_delete)
            val failDesc = stringResource(R.string.not_sent_desc)
            Box {
                Box(Modifier.padding(end = 6.dp).size(28.dp).background(cs.error, CircleShape).testTag("failed-mark")
                    .clickable(onClickLabel = retryLabel) { ask = true }
                    .semantics { contentDescription = failDesc },
                    contentAlignment = Alignment.Center) { Text("!", color = cs.onError, style = MaterialTheme.typography.labelLarge) }
                DropdownMenu(expanded = ask, onDismissRequest = { ask = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.retry)) }, modifier = Modifier.testTag("retry"), onClick = { ask = false; onRetry(failedP) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, modifier = Modifier.testTag("delete"), onClick = { ask = false; onDiscard(failedP) })
                }
            }
        }
        // Send later (AII-146): a clock mark; tap it to send now or delete.
        val laterP = r.pending?.takeIf { b.state == SendState.LATER }
        if (laterP != null) {
            var ask by remember { mutableStateOf(false) }
            val laterLabel = stringResource(R.string.later_options)
            Box {
                Box(Modifier.padding(end = 6.dp).size(28.dp).background(cs.secondaryContainer, CircleShape).testTag("later-mark")
                    .clickable(onClickLabel = laterLabel) { ask = true }
                    .semantics { contentDescription = laterLabel },
                    contentAlignment = Alignment.Center) { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp), tint = cs.onSecondaryContainer) }
                DropdownMenu(expanded = ask, onDismissRequest = { ask = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.send_now)) }, modifier = Modifier.testTag("send-now"), onClick = { ask = false; onSendNow(laterP) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, modifier = Modifier.testTag("delete"), onClick = { ask = false; onDiscard(laterP) })
                }
            }
        }
        Surface(
            color = bg,
            border = when (mark) { 2 -> BorderStroke(3.dp, cs.tertiary); 1 -> BorderStroke(1.5.dp, cs.tertiary.copy(alpha = 0.6f)); else -> null },   // AII-78: find matches
            shape = RoundedCornerShape(18.dp, 18.dp, if (b.mine) 4.dp else 18.dp, if (b.mine) 18.dp else 4.dp),
            modifier = Modifier.widthIn(max = 320.dp).semantics(mergeDescendants = true) {}.testTag(if (b.mine) "mine" else "theirs")
                .combinedClickable(onClick = { showTime = !showTime }, onLongClick = { menu = true }),
        ) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(R.string.reply)) }, modifier = Modifier.testTag("reply"),
                    onClick = { menu = false; onReply(b) })
                if (b.state == SendState.SENT && !b.unverified && b.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.forward)) }, modifier = Modifier.testTag("forward"),
                    onClick = { menu = false; onForward(b) })
                // AII-138: star it, to find it again in the chat details.
                if (onStar != null && b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(if (starred) R.string.unstar else R.string.star)) },
                    modifier = Modifier.testTag("star"), onClick = { menu = false; onStar() })
                DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, modifier = Modifier.testTag("copy"), onClick = {
                    menu = false
                    clipboard.setText(AnnotatedString(shown.text))
                    // Android 13+ shows its own "copied" note; a toast as well would say it twice.
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
                })
                if (shown.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.select_text)) }, modifier = Modifier.testTag("select"),
                    onClick = { menu = false; selecting = true })
                // AII-129: copy a link in the message on its own; several links get one line each.
                val links = Markdown.links(shown)
                links.forEach { url ->
                    DropdownMenuItem(text = { Text(if (links.size == 1) stringResource(R.string.copy_link) else stringResource(R.string.copy_link_named, Markdown.shortLink(url)), maxLines = 1) },
                        modifier = Modifier.testTag("copy-link"), onClick = {
                            menu = false
                            clipboard.setText(AnnotatedString(url))
                            if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
                        })
                }
                // AII-121: out to another app (email, notes...) through the phone's share menu.
                if (shown.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, modifier = Modifier.testTag("share"), onClick = {
                    menu = false
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, shown.text)
                    runCatching { ctx.startActivity(android.content.Intent.createChooser(send, null)) }
                })
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (!b.mine && ref.kind != Kind.FRIEND && b.author.isNotBlank()) {
                    Text(b.author, style = MaterialTheme.typography.labelMedium, color = cs.primary)
                }
                if (b.pics.isNotEmpty()) MessagePictures(b.pics, repo)
                val (forwarded, body) = Forward.split(b.text)
                if (forwarded) Text(stringResource(R.string.forwarded_label), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.6f), modifier = Modifier.testTag("forwarded"))
                val (quoted, rest) = Quote.split(body)
                // AII-99: tapping the quote jumps to the message it quotes.
                if (quoted != null) QuoteBar(Markdown.render(quoted, cs.surfaceVariant, cs.primary), fg,
                    Modifier.clickable(onClickLabel = stringResource(R.string.show_quoted)) { onQuote(quoted) }.testTag("quote"))
                if (quoted != null && rest.isNotEmpty()) Text(Markdown.render(rest, cs.surfaceVariant, cs.primary, cards, onCard, copy), style = MaterialTheme.typography.bodyLarge, color = fg)
                else if (quoted == null && body.isNotEmpty()) Text(if (forwarded) Markdown.render(body, cs.surfaceVariant, cs.primary, cards, onCard, copy) else shown, style = MaterialTheme.typography.bodyLarge, color = fg)
                Text(
                    when (b.state) {
                        SendState.SENDING -> stringResource(R.string.sending)
                        SendState.FAILED -> stringResource(R.string.not_sent)
                        SendState.LATER -> stringResource(R.string.sends_at, clock(b.at))
                        SendState.SENT -> (if (starred) "★ " else "") + clock(b.at) +if (ref.kind == Kind.FRIEND && !b.unverified) "  🔒" else ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (b.state == SendState.FAILED) cs.error else fg.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        }
        if (selecting) SelectText(shown.text) { selecting = false }
        if (showTime && b.at.isNotBlank()) Text(fullTime(b.at), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("fulltime"))
        if (r.showOptions) FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            b.options.forEach { o -> OutlinedButton(onClick = { onOption(o) }) { Text(o) } }
        }
    }
    }
}

/** The message on its own, so you can drag the handles over just the part you want and copy it (AII-127). */
@Composable
private fun SelectText(text: String, onClose: () -> Unit) = AlertDialog(
    onDismissRequest = onClose,
    title = { Text(stringResource(R.string.select_text)) },
    text = {
        SelectionContainer(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("select-text"))
        }
    },
    confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.done)) } },
)

private val SWIPE_REPLY = 72.dp

/** How far a reply swipe has got, 0 to 1; at 1 letting go starts the reply (AII-101). */
internal fun replyProgress(drag: Float, reach: Float): Float = if (reach <= 0f) 0f else (drag / reach).coerceIn(0f, 1f)
