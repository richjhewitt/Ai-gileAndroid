package com.smizflark.aigile.messenger.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.chatInfo
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Forward
import com.smizflark.aigile.messenger.data.Quote
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Stars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.snapshotFlow
import com.smizflark.aigile.messenger.data.ScrollPlace
import com.smizflark.aigile.messenger.data.ScrollPlaces
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.platform.LocalView

/**
 * Journey 3: read a chat and reply. Replies go to the question waiting on you, a card you pick, or a new 💬.
 * With [focus] (AII-28) it shows one card of the chat: only its messages, and replies go to that card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repo: Repo, ref: ChatRef, onBack: () -> Unit, focus: String? = null, onOpenCard: (String) -> Unit = {}, openAt: String? = null) {
    var convo by remember { mutableStateOf(repo.store.cachedConversation(ref.key)) }
    val seenAtOpen = rememberSaveable(ref.key) { mutableStateOf(repo.store.seen()[ref.key]) }.value   // AII-100: the "New messages" line stays put while you read
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
    var stars by remember { mutableStateOf(repo.store.stars()) }   // AII-138: starred messages
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

    // "working…" under the name while the team is at work (AII-112), "working on AII-5" when we know the card (AII-113):
    // asked of the chats list every 20 s, only on screen.
    var working by remember(ref.key) { mutableStateOf(ref.working) }
    var workingOn by remember(ref.key) { mutableStateOf(ref.workingOn) }
    if (ref.kind == Kind.LEAD && focus == null) LaunchedEffect(ref.key) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { repo.fresh(ref)?.let { working = it.working; workingOn = it.workingOn }; delay(20_000) } }
    }

    val items = remember(convo, pending, owner, focus) { rows(convo, pending, owner, focus, seenAtOpen).asReversed() }
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
    // AII-117: while you scroll by hand, the day of the topmost row floats at the top, and fades a little after.
    // Jumps (newest, a quote, a find hit) don't show it.
    val days = remember(items) { daysAbove(items) }
    val topDay by remember(days) { derivedStateOf { list.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index }?.let { days.getOrNull(it - 1) } } }   // index 0 is the spacer
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var scrolling by remember { mutableStateOf(false) }
    LaunchedEffect(dragged, list.isScrollInProgress) {
        if (dragged) scrolling = true
        else if (scrolling && !list.isScrollInProgress) { delay(1200); scrolling = false }   // a fling after the drag keeps it up
    }
    // AII-72: coming back, the chat opens where you left it; the 'N new' pill counts from what you had seen.
    val placeKey = ref.key + (focus?.let { "/$it" } ?: "")
    val place = remember(placeKey) { if (openAt != null) null else ScrollPlaces.get(placeKey) }   // a search result wins (AII-105)
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
    // AII-102: more new messages than fit on screen? Open at the "New messages" line, not the bottom.
    LaunchedEffect(placeKey) {
        if (place != null || openAt != null) return@LaunchedEffect
        val i = snapshotFlow { latestItems.indexOfFirst { it is Row.New } }.first { it >= 0 } + 1   // +1: the spacer at the bottom
        val info = snapshotFlow { list.layoutInfo }.first { it.totalItemsCount > i && it.visibleItemsInfo.isNotEmpty() }
        if (list.firstVisibleItemIndex > 1 || info.visibleItemsInfo.any { it.index == i }) return@LaunchedEffect
        list.scrollToItem(i)
        val line = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == i }?.size ?: 0
        list.scrollBy(-(info.viewportEndOffset - info.viewportStartOffset - line).toFloat())
    }
    val unread = if (atBottom) 0 else unreadBelow(convo?.bubbles.orEmpty(), seenNewest)
    // AII-99: tap a quote, the chat scrolls to the quoted message and outlines it for a moment.
    var flash by remember { mutableStateOf<String?>(null) }
    fun jumpToQuote(quoted: String, from: String) {
        val i = Quote.source(quoted, items.indexOfFirst { it.id == from }, items.map { (it as? Row.Msg)?.b }, owner) ?: return
        val id = items[i].id
        scope.launch { list.animateScrollToItem(i + 1); flash = id; delay(1500); if (flash == id) flash = null }   // +1: the spacer at the bottom
    }
    // AII-105: opened from a search result: scroll to that message and outline it.
    LaunchedEffect(openAt) {
        if (openAt == null) return@LaunchedEffect
        val i = snapshotFlow { latestItems.indexOfFirst { it.id == openAt } }.first { it >= 0 }
        list.scrollToItem(i + 1); flash = openAt; delay(1500); if (flash == openAt) flash = null   // +1: the spacer at the bottom
    }

    // AII-138: tap a starred message in the details, the chat scrolls to it and outlines it.
    fun jumpToStar(key: String) {
        val i = items.indexOfFirst { it.id == key }
        if (i < 0) { Toast.makeText(view.context, R.string.star_not_loaded, Toast.LENGTH_SHORT).show(); return }
        scope.launch { list.animateScrollToItem(i + 1); flash = key; delay(1500); if (flash == key) flash = null }   // +1: the spacer at the bottom
    }

    if (details) ChatDetails(ref, repo, chatInfo(convo), onFind = { details = false; finding = true },
        starred = Stars.inChat(stars, ref.key), onStarred = { details = false; jumpToStar(it) }) { details = false; muted = ref.key in repo.store.muted() }
    Scaffold(
        topBar = {
            ChatTopBar(ref, repo, focus, focusBubble, working, workingOn, muted, onBack = onBack, onDetails = { details = true },
                onFind = { finding = true }, onMute = { muted = !muted; repo.store.setMuted(ref.key, muted) })
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
                            is Row.New -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("new-line"), verticalAlignment = Alignment.CenterVertically) {
                                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.new_messages), Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                            }
                            is Row.Card -> CardHeader(r, chosen = target == r.card && focus == null,
                                onOpen = if (focus == null && ref.kind == Kind.LEAD) { { onOpenCard(r.card) } } else null) {
                                if (targets.any { it.id == r.card }) { target = r.card; picked = true }
                            }
                            is Row.Msg -> MessageBubble(r, ref, repo, mark = if (r.id == flash) 2 else if (r.id !in hitIds) 0 else if (hitIds.getOrNull(current) == r.id) 2 else 1, onReply = { q ->
                                quoting = Quote.of(if (q.mine) owner else q.author, q.text, q.pics.size)
                                // A reply to a team message goes to its card, like answering it on the board.
                                if (ref.kind == Kind.LEAD && focus == null && q.card != null && targets.any { it.id == q.card }) { target = q.card; picked = true }
                            }, onForward = { forwarding = it.text }, cards = if (ref.kind == Kind.LEAD) known - setOfNotNull(focus) else emptySet(), onCard = onOpenCard,
                                onOption = { send(it, r.b.card) },
                                onRetry = { p -> deliver(p) },
                                onDiscard = { p -> repo.discard(p.id); pending = repo.pendingFor(ref.key) },
                                onQuote = { q -> jumpToQuote(q, r.id) },
                                starred = Stars.has(stars, ref.key, r.b.key),
                                onStar = if (r.pending == null) { { stars = Stars.toggle(stars, Stars.of(ref.key, r.b)); repo.store.setStars(stars) } } else null)
                        }
                    }
                    if (convo == null && error == null) item { Text(stringResource(R.string.loading), Modifier.padding(24.dp)) }
                    detail?.let { d -> item(key = "card-details") { CardDetails(d) } }   // last item = top of the reversed list
                    if (convo != null && convo!!.bubbles.isEmpty() && pending.isEmpty()) item {
                        Text(stringResource(R.string.chat_empty), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DayPill(if (scrolling) topDay else null)
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
