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
import com.smizflark.aigile.messenger.data.chatInfo
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
    var details by remember { mutableStateOf(false) }   // AII-80: the chat details sheet
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

    if (details) ChatDetails(ref, repo, chatInfo(convo)) { details = false; muted = ref.key in repo.store.muted() }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { details = true }.testTag("chat-title")) {
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
