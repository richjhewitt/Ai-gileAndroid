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

private sealed interface Row {
    val id: String
    data class Day(override val id: String, val label: String) : Row
    data class Card(override val id: String, val card: String, val title: String, val waiting: Boolean) : Row
    data class Msg(override val id: String, val b: Bubble, val pending: Pending? = null, val showOptions: Boolean = false) : Row
}

/** Bubbles, pending messages, day lines and card headers, oldest first. */
/** Messages from them after the newest one you saw at the bottom of the chat (AII-46). */
internal fun unreadBelow(bubbles: List<Bubble>, seen: String?): Int {
    val at = bubbles.indexOfLast { it.key == seen }
    if (seen == null || at < 0) return 0
    return bubbles.subList(at + 1, bubbles.size).count { !it.mine }
}

private fun rows(c: Conversation?, pending: List<Pending>, owner: String, focus: String? = null): List<Row> {
    val out = mutableListOf<Row>()
    val bubbles = c?.bubbles.orEmpty().filter { focus == null || it.card == focus }
    // Answer buttons only under the newest message of a card still waiting on you.
    val optionsAt = bubbles.withIndex().filter { it.value.options.isNotEmpty() && it.value.waiting }
        .groupBy { it.value.card }.mapNotNull { (_, l) -> l.maxOf { it.index } }.toSet()
    var day: java.time.LocalDate? = null
    var card: String? = null
    bubbles.forEachIndexed { i, b ->
        val d = dayOf(b.at)
        if (d != null && d != day) { out += Row.Day("day:$d", dayLabel(b.at)); day = d; card = null }
        if (b.card != null && b.card != card) {
            out += Row.Card("card:$i:${b.card}", b.card, Mapper.clean(b.cardTitle ?: ""), b.waiting)
        }
        card = b.card
        out += Row.Msg(b.key, b, showOptions = i in optionsAt)
    }
    pending.filter { focus == null || it.target == focus }.forEach { p ->
        out += Row.Msg("p:${p.id}", Bubble("p:${p.id}", owner, p.text, p.at, true, p.target,
            state = if (p.failed && !p.offline) SendState.FAILED else SendState.SENDING,   // offline ones go by themselves (AII-55)
            pics = p.files.map { Pic(File(it).toURI().toString(), File(it).name, "image/") }), p)
    }
    return out
}

/**
 * Journey 3: read a chat and reply. Replies go to the question waiting on you, a card you pick, or a new 💬.
 * With [focus] (AII-28) it shows one card of the chat: only its messages, and replies go to that card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repo: Repo, ref: ChatRef, onBack: () -> Unit, focus: String? = null, onOpenCard: (String) -> Unit = {}) {
    var convo by remember { mutableStateOf(repo.store.cachedConversation(ref.key)) }
    var pending by remember { mutableStateOf(repo.pendingFor(ref.key)) }
    var target by rememberSaveable { mutableStateOf(focus ?: convo?.defaultTarget) }
    var picked by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf(repo.store.draft(ref.key)) }
    var pics by rememberSaveable { mutableStateOf(listOf<String>()) }
    var picNote by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<CardDetail?>(null) }
    if (focus != null && ref.kind == Kind.LEAD) LaunchedEffect(focus) { detail = repo.card(ref, focus) }
    var quoting by rememberSaveable { mutableStateOf<String?>(null) }   // AII-60: the quoted line of the message being answered
    var forwarding by remember { mutableStateOf<String?>(null) }   // AII-71: the text of the message being forwarded
    var error by remember { mutableStateOf<String?>(null) }
    var muted by remember { mutableStateOf(ref.key in repo.store.muted()) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    var finding by rememberSaveable { mutableStateOf(false) }   // AII-78: the find bar
    var query by rememberSaveable { mutableStateOf("") }
    var at by rememberSaveable { mutableStateOf(0) }   // which match, 0 = the newest
    // AII-49: a little tick on send and when a reply lands. performHapticFeedback follows the phone's touch feedback setting.
    val view = LocalView.current
    fun tick() = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    val owner = repo.store.owner.ifBlank { "You" }
    val res = LocalContext.current.resources

    // Keep the half-typed reply (AII-42): saved a moment after typing stops, and on the way out.
    LaunchedEffect(text) { delay(400); repo.store.saveDraft(ref.key, text) }
    val latest by rememberUpdatedState(text)
    DisposableEffect(ref.key) { onDispose { repo.store.saveDraft(ref.key, latest) } }

    val pace = remember { Pace(fast = 3_000, slow = 30_000) }

    /** Fetches the chat; true when there is something new. */
    suspend fun load(): Boolean {
        try {
            val c = repo.conversation(ref)
            val changed = c.bubbles != convo?.bubbles
            val before = convo?.bubbles?.lastOrNull { !it.mine }?.at
            val now = c.bubbles.lastOrNull { !it.mine }?.at
            if (before != null && now != null && now > before) tick()
            convo = c; error = null
            if (!picked && focus == null) target = c.defaultTarget
            repo.seen(ref, c.bubbles.lastOrNull()?.at)
            return changed
        } catch (e: Exception) { error = e.message; return false }
    }

    fun deliver(p: Pending) {
        pending = repo.pendingFor(ref.key)
        scope.launch {
            val r = if (p.failed) repo.retry(ref, p) else repo.deliver(ref, p)
            r.onSuccess { card ->
                // A new 💬 carries on in its own card, like the board's page (AIG-757).
                if (ref.kind == Kind.LEAD && p.target == null && card != null && focus == null) { target = card; picked = true }
                load(); pace.poke()   // a reply is likely soon: look quickly for a while
            }.onFailure { error = it.message }
            pending = repo.pendingFor(ref.key)
            list.animateScrollToItem(0)
        }
    }

    fun send(body: String, to: String?, files: List<String> = emptyList()) {
        val t = body.trim(); if (t.isEmpty() && files.isEmpty()) return
        tick()
        deliver(repo.queue(ref, t, to, files))
    }

    // Pictures (AII-27): the system photo picker needs no permission; each pick is copied into the app at once.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICS)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            picNote = res.getQuantityString(R.plurals.adding_pictures, uris.size, uris.size)
            val kept = withContext(Dispatchers.IO) { uris.map { repo.keepPicture(it) } }
            pics = (pics + kept.filterNotNull()).take(MAX_PICS)
            picNote = if (kept.any { it == null }) res.getString(R.string.pic_read_one) else null
        }
    }

    // A copied picture (AII-56): offered as a chip above the message box.
    val copied = rememberCopiedPicture()
    fun pasteCopied() {
        val uri = copied.take() ?: run { picNote = res.getString(R.string.pic_read_copied); return }
        scope.launch {
            val kept = withContext(Dispatchers.IO) { repo.keepPicture(uri) }
            if (kept == null) picNote = res.getString(R.string.pic_read_copied) else { pics = (pics + kept).take(MAX_PICS); picNote = null }
        }
    }

    forwarding?.let { body ->
        val ctx = LocalContext.current
        ForwardDialog(repo, onDismiss = { forwarding = null }) { to ->
            forwarding = null
            // The normal send path: queued first, so it waits and retries if the phone is offline.
            val p = repo.queue(to, Forward.text(body), null)
            if (to.key == ref.key) pending = repo.pendingFor(ref.key)
            scope.launch {
                val r = repo.deliver(to, p)
                if (to.key == ref.key) { load(); pending = repo.pendingFor(ref.key) }
                Toast.makeText(ctx, ctx.getString(if (r.isSuccess) R.string.forwarded_to else R.string.forward_queued, to.name), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ref.key) {
        // Anything left from last time goes first (it was saved before sending, so nothing is lost).
        repo.pendingFor(ref.key).filter { !it.failed || it.offline }.forEach { deliver(it) }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Live: every 3 s while the chat is busy, easing off to 30 s when it is quiet; never in the background.
            while (true) { pace.after(load()); pace.sleep() }
        }
    }

    val items = remember(convo, pending, owner, focus) { rows(convo, pending, owner, focus).asReversed() }
    val hits = remember(items, query, finding) { if (finding) Find.matches(items.map { (it as? Row.Msg)?.b?.text.orEmpty() }, query) else emptyList() }
    val hitIds = remember(hits, items) { hits.map { items[it].id } }
    LaunchedEffect(query) { at = 0 }
    val current = at.coerceIn(0, (hits.size - 1).coerceAtLeast(0))
    LaunchedEffect(finding, query, hits.size, current) { if (hits.isNotEmpty()) list.animateScrollToItem(hits[current] + 1) }   // +1: the spacer at the bottom
    val targets = if (focus != null) emptyList() else convo?.targets.orEmpty()
    // Card ids in messages open that card, when it is one of this chat's (AII-28).
    val known = convo?.bubbles.orEmpty().mapNotNull { it.card }.toSet()
    val focusBubble = focus?.let { f -> convo?.bubbles?.lastOrNull { it.card == f } }
    val atBottom by remember { derivedStateOf { list.firstVisibleItemIndex <= 1 } }
    // AII-72: coming back, the chat opens where you left it; the 'N new' pill counts from what you had seen.
    val placeKey = ref.key + (focus?.let { "/$it" } ?: "")
    val place = remember(placeKey) { ScrollPlaces.get(placeKey) }
    var restoring by remember(placeKey) { mutableStateOf(place != null) }
    var seenNewest by rememberSaveable { mutableStateOf(place?.seen) }
    val newest = convo?.bubbles?.lastOrNull()?.key
    LaunchedEffect(atBottom, newest, restoring) { if (atBottom && !restoring) seenNewest = newest }
    val latestItems by rememberUpdatedState(items)
    val latestSeen by rememberUpdatedState(seenNewest)
    LaunchedEffect(placeKey) {
        if (place != null) {
            snapshotFlow { latestItems.size }.first { it > 0 }
            val at = ScrollPlaces.indexOf(place, latestItems.map { it.id })
            if (at != null) list.scrollToItem(at + 1, place.offset)   // +1: the spacer at the bottom
            restoring = false
        }
        snapshotFlow { Triple(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset, latestItems.size) }.collect { (i, off, n) ->
            if (n == 0) return@collect
            if (i <= 1) ScrollPlaces.clear(placeKey)
            else (list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == i }?.key as? String)
                ?.let { ScrollPlaces.save(placeKey, ScrollPlace(it, off, latestSeen)) }
        }
    }
    val unread = if (atBottom) 0 else unreadBelow(convo?.bubbles.orEmpty(), seenNewest)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(ref, repo, 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(if (focus != null) "$focus ${Mapper.clean(focusBubble?.cardTitle ?: "")}" else ref.name,
                                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (focus == null) ref.subtitle else ref.name + if (focusBubble?.waiting == true) " · ⏳ " + stringResource(R.string.waiting_on_you) else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                actions = {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { finding = true }) { Icon(Icons.Default.Search, stringResource(R.string.find_in_chat)) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (muted) R.string.unmute else R.string.mute_alerts)) },
                            leadingIcon = { Icon(if (muted) Icons.Default.Notifications else Icons.Default.NotificationsOff, null) },
                            onClick = { muted = !muted; repo.store.setMuted(ref.key, muted); menu = false },
                            modifier = Modifier.testTag("mute"),
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(10.dp).testTag("error"))
            }
            if (finding) FindBar(query, { query = it }, hits.size, current,
                onUp = { if (hits.isNotEmpty()) at = (current + 1) % hits.size },   // older
                onDown = { if (hits.isNotEmpty()) at = (current - 1 + hits.size) % hits.size },   // newer
                onClose = { finding = false; query = "" })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize(), state = list, reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Spacer(Modifier.size(8.dp)) }
                    items(items, key = { it.id }) { r ->
                        when (r) {
                            is Row.Day -> Text(r.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            is Row.Card -> CardHeader(r, chosen = target == r.card && focus == null,
                                onOpen = if (focus == null && ref.kind == Kind.LEAD) { { onOpenCard(r.card) } } else null) {
                                if (targets.any { it.id == r.card }) { target = r.card; picked = true }
                            }
                            is Row.Msg -> MessageBubble(r, ref, repo, mark = if (r.id !in hitIds) 0 else if (hitIds.getOrNull(current) == r.id) 2 else 1, onReply = { q ->
                                quoting = Quote.of(if (q.mine) owner else q.author, q.text, q.pics.size)
                                // A reply to a team message goes to its card, like answering it on the board.
                                if (ref.kind == Kind.LEAD && focus == null && q.card != null && targets.any { it.id == q.card }) { target = q.card; picked = true }
                            }, onForward = { forwarding = it.text }, cards = if (ref.kind == Kind.LEAD) known - setOfNotNull(focus) else emptySet(), onCard = onOpenCard,
                                onOption = { send(it, r.b.card) },
                                onRetry = { p -> deliver(p) },
                                onDiscard = { p -> repo.discard(p.id); pending = repo.pendingFor(ref.key) })
                        }
                    }
                    if (convo == null && error == null) item { Text(stringResource(R.string.loading), Modifier.padding(24.dp)) }
                    detail?.let { d -> item(key = "card-details") { CardDetails(d) } }   // last item = top of the reversed list
                    if (convo != null && convo!!.bubbles.isEmpty() && pending.isEmpty()) item {
                        Text(stringResource(R.string.chat_empty), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                JumpToNewest(show = !atBottom && convo != null, unread = unread) { scope.launch { list.animateScrollToItem(0) } }
            }
            Composer(ref, text, { text = it }, targets.map { it.id to it.label }, target, { target = it; picked = true },
                pics = pics, picNote = picNote,
                onAttach = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemovePic = { f -> pics = pics - f; File(f).delete() },
                onPasteCopied = if (copied.available && pics.size < MAX_PICS) ::pasteCopied else null,
                quote = quoting, onClearQuote = { quoting = null },
            ) {
                send(Quote.with(quoting, text.trim()).takeIf { text.isNotBlank() } ?: text, if (ref.kind == Kind.LEAD) focus ?: target else null, pics); text = ""; pics = emptyList(); picNote = null; quoting = null
            }
        }
    }
}

/** AII-78: the find bar: a text field, "2 of 5", previous / next match and close. */
@Composable
private fun FindBar(query: String, onQuery: (String) -> Unit, count: Int, current: Int, onUp: () -> Unit, onDown: () -> Unit, onClose: () -> Unit) {
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
private fun BoxScope.JumpToNewest(show: Boolean, unread: Int, onClick: () -> Unit) {
    if (!show) return
    if (unread > 0) ExtendedFloatingActionButton(
        onClick = onClick, icon = { Icon(Icons.Default.ArrowDownward, null) }, text = { Text(stringResource(R.string.jump_unread, unread)) },
        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).testTag("jump"),
    ) else SmallFloatingActionButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).testTag("jump")) {
        Icon(Icons.Default.ArrowDownward, stringResource(R.string.jump_newest))
    }
}

@Composable
private fun CardHeader(r: Row.Card, chosen: Boolean, onOpen: (() -> Unit)?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onClick, shape = RoundedCornerShape(50),
            color = if (r.waiting) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                (if (r.waiting) "⏳ " else "") + "${r.card}  ${r.title}" + if (chosen) "  ✓" else "",
                style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 260.dp),
            )
        }
        if (onOpen != null) IconButton(onClick = onOpen, modifier = Modifier.size(40.dp).testTag("open:${r.card}")) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.open_card, r.card), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(r: Row.Msg, ref: ChatRef, repo: Repo, mark: Int, onReply: (Bubble) -> Unit, onForward: (Bubble) -> Unit, cards: Set<String>, onCard: (String) -> Unit, onOption: (String) -> Unit, onRetry: (Pending) -> Unit, onDiscard: (Pending) -> Unit) {
    val b = r.b
    val cs = MaterialTheme.colorScheme
    val bg = when {
        b.unverified -> cs.errorContainer
        b.mine -> cs.primaryContainer
        else -> cs.surfaceContainerHigh
    }
    val fg = if (b.mine) cs.onPrimaryContainer else cs.onSurface
    val shown = Markdown.render(b.text, cs.surfaceVariant, cs.primary, cards, onCard)
    // AII-43: long-press a bubble to copy what it shows (without the ** marks).
    var menu by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }   // AII-51: tap a bubble for its exact time
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalAlignment = if (b.mine) Alignment.End else Alignment.Start) {
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
        Surface(
            color = bg,
            border = when (mark) { 2 -> BorderStroke(3.dp, cs.tertiary); 1 -> BorderStroke(1.5.dp, cs.tertiary.copy(alpha = 0.6f)); else -> null },   // AII-78: find matches
            shape =RoundedCornerShape(18.dp, 18.dp, if (b.mine) 4.dp else 18.dp, if (b.mine) 18.dp else 4.dp),
            modifier = Modifier.widthIn(max = 320.dp).semantics(mergeDescendants = true) {}.testTag(if (b.mine) "mine" else "theirs")
                .combinedClickable(onClick = { showTime = !showTime }, onLongClick = { menu = true }),
        ) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(R.string.reply)) }, modifier = Modifier.testTag("reply"),
                    onClick = { menu = false; onReply(b) })
                if (b.state == SendState.SENT && !b.unverified && b.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.forward)) }, modifier = Modifier.testTag("forward"),
                    onClick = { menu = false; onForward(b) })
                DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, modifier = Modifier.testTag("copy"), onClick = {
                    menu = false
                    clipboard.setText(AnnotatedString(shown.text))
                    // Android 13+ shows its own "copied" note; a toast as well would say it twice.
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
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
                if (quoted != null) QuoteBar(Markdown.render(quoted, cs.surfaceVariant, cs.primary), fg)
                if (quoted != null && rest.isNotEmpty()) Text(Markdown.render(rest, cs.surfaceVariant, cs.primary, cards, onCard), style = MaterialTheme.typography.bodyLarge, color = fg)
                else if (quoted == null && body.isNotEmpty()) Text(if (forwarded) Markdown.render(body, cs.surfaceVariant, cs.primary, cards, onCard) else shown, style = MaterialTheme.typography.bodyLarge, color = fg)
                Text(
                    when (b.state) {
                        SendState.SENDING -> stringResource(R.string.sending)
                        SendState.FAILED -> stringResource(R.string.not_sent)
                        SendState.SENT -> clock(b.at) + if (ref.kind == Kind.FRIEND && !b.unverified) "  🔒" else ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (b.state == SendState.FAILED) cs.error else fg.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        }
        if (showTime && b.at.isNotBlank()) Text(fullTime(b.at), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("fulltime"))
        if (r.showOptions) FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            b.options.forEach { o -> OutlinedButton(onClick = { onOption(o) }) { Text(o) } }
        }
    }
}

@Composable
private fun Composer(
    ref: ChatRef, text: String, onText: (String) -> Unit,
    targets: List<Pair<String?, String>>, target: String?, onTarget: (String?) -> Unit,
    pics: List<String> = emptyList(), picNote: String? = null, onAttach: () -> Unit = {}, onRemovePic: (String) -> Unit = {},
    onPasteCopied: (() -> Unit)? = null,
    quote: String? = null, onClearQuote: () -> Unit = {},
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (ref.kind == Kind.LEAD && targets.isNotEmpty()) {
                var open by remember { mutableStateOf(false) }
                val newMessage = stringResource(R.string.new_message)
                val label = targets.firstOrNull { it.first == target && it.first != null }?.second ?: newMessage
                Box {
                    AssistChip(
                        onClick = { open = true },
                        label = { Text(stringResource(R.string.replying_to, label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.testTag("target"),
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        targets.forEach { (id, l) ->
                            DropdownMenuItem(text = { Text(if (id == null) "✏️ $newMessage" else l, maxLines = 2) }, onClick = { onTarget(id); open = false })
                        }
                    }
                }
            }
            picNote?.let { PictureNote(it) }
            if (quote != null) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("quoting"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { QuoteBar(Markdown.render(Quote.split(quote).first ?: quote, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary), MaterialTheme.colorScheme.onSurface) }
                IconButton(onClick = onClearQuote, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Close, stringResource(R.string.quote_cancel)) }
            }
            if (onPasteCopied != null) AssistChip(
                onClick = onPasteCopied, label = { Text(stringResource(R.string.paste_picture)) },
                leadingIcon = { Icon(Icons.Default.ContentPaste, null) }, modifier = Modifier.testTag("paste-pic"),
            )
            if (pics.isNotEmpty()) PickedPictures(pics, onRemovePic)
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = onAttach, enabled = pics.size < MAX_PICS, modifier = Modifier.size(52.dp).testTag("attach")) {
                    Icon(Icons.Default.AddPhotoAlternate, stringResource(R.string.add_picture))
                }
                OutlinedTextField(
                    value = text, onValueChange = onText, maxLines = 6,
                    placeholder = { Text(stringResource(if (ref.kind == Kind.WATCHMAN) R.string.message_watchman else R.string.message)) },
                    trailingIcon = { MicButton { onText(Dictate.join(text, it)) } },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("input"),
                )
                Spacer(Modifier.size(8.dp))
                FilledIconButton(onClick = onSend, enabled = text.isNotBlank() || pics.isNotEmpty(), modifier = Modifier.size(52.dp).testTag("send")) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send))
                }
            }
        }
    }
}

/** Pictures on one message: plenty for screenshots, and well inside the board's 20 files a comment. */
private const val MAX_PICS = 6

/** A quoted line (AII-60): a coloured bar, then the quote, a little quieter than the message. */
@Composable
private fun QuoteBar(text: AnnotatedString, fg: androidx.compose.ui.graphics.Color) {
    Row(Modifier.padding(bottom = 4.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.75f), maxLines = 3,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Pick the chat to forward to (AII-71): the chats list with a search box. */
@Composable
private fun ForwardDialog(repo: Repo, onDismiss: () -> Unit, onPick: (ChatRef) -> Unit) {
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
