package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.work.waitingToSend
import java.time.Instant
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.smizflark.aigile.messenger.data.Held
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.chatInfo
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Quote
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.PinnedMsgs
import com.smizflark.aigile.messenger.data.Stars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.snapshotFlow
import com.smizflark.aigile.messenger.data.ScrollPlaces
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalView

/**
 * Journey 3: read a chat and reply. Replies go to the question waiting on you, a card you pick, or a new 💬.
 * With [focus] (AII-28) it shows one card of the chat: only its messages, and replies go to that card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repo: Repo, ref: ChatRef, onBack: () -> Unit, focus: String? = null, onOpenCard: (String) -> Unit = {}, openAt: String? = null, onOpenChat: (ChatRef) -> Unit = {}, onEditReplies: () -> Unit = {}) {
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
    var replyId by rememberSaveable { mutableStateOf<String?>(null) }   // AII-324: friend chats send the id of the line being answered
    var forwarding by remember { mutableStateOf<String?>(null) }   // AII-71: the text of the message being forwarded
    var error by remember { mutableStateOf<String?>(null) }
    var muted by remember { mutableStateOf(ref.key in repo.store.muted()) }
    var silent by remember { mutableStateOf(ref.key in repo.store.silent) }
    var muteAsk by remember { mutableStateOf(false) }   // AII-157: Mute asks how long
    var reminders by remember { mutableStateOf(repo.store.reminders()) }   // AII-251: read once here, not per bubble
    var details by remember { mutableStateOf(false) }   // AII-80: the chat details sheet
    var stars by remember { mutableStateOf(repo.store.stars()) }   // AII-138: starred messages
    var pins by remember { mutableStateOf(repo.store.pinnedMsgs()) }   // AII-339: pinned messages, this phone only
    var clearedAt by remember(ref.key) { mutableStateOf(repo.store.cleared()[ref.key]) }   // AII-363: Clear chat for me, this phone only
    var clearAsk by remember { mutableStateOf(false) }
    var pinShown by remember { mutableStateOf(0) }   // AII-339: which pin the bar shows
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    var finding by rememberSaveable { mutableStateOf(false) }   // AII-78: the find bar
    var query by rememberSaveable { mutableStateOf("") }
    var at by rememberSaveable { mutableStateOf(0) }   // which match, 0 = the newest
    // AII-49: a little tick on send and when a reply lands. performHapticFeedback follows the phone's touch feedback setting.
    val view = LocalView.current
    fun tick() = Appearance.buzz && view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    val owner = repo.store.owner.ifBlank { "You" }
    val res = LocalContext.current.resources

    // Keep the half-typed reply (AII-42): saved a moment after typing stops, and on the way out.
    LaunchedEffect(text) { delay(400); repo.store.saveDraft(ref.key, text) }
    val latest by rememberUpdatedState(text)
    DisposableEffect(ref.key) { onDispose { repo.store.saveDraft(ref.key, latest) } }
    DisposableEffect(ref.key) { onDispose { ReadAloud.stop(); VoicePlayer.stop() } }   // AII-331: and a playing voice note   // AII-185: leaving the chat stops Read aloud

    val pace = remember { Pace(fast = 3_000, slow = 30_000) }

    /** Fetches the chat; true when there is something new. */
    suspend fun load(): Boolean {
        try {
            val c = repo.conversation(ref)
            val changed = c.bubbles != convo?.bubbles
            val before = convo?.bubbles?.lastOrNull { !it.mine }?.at
            val now = c.bubbles.lastOrNull { !it.mine }?.at
            if (before != null && now != null && now > before) tick()
            convo = c; error = null; stars = repo.store.stars()   // AII-141: the board's stars came with it
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

    fun send(body: String, to: String?, files: List<String> = emptyList(), reply: String? = null) {
        val t = body.trim(); if (t.isEmpty() && files.isEmpty()) return
        tick()
        deliver(repo.queue(ref, t, to, files, reply = reply))
    }

    val voice = if (ref.kind == Kind.FRIEND) rememberVoiceController { send("", null, listOf(it)) } else null   // AII-331 (VoiceRecord.kt)

    /* ---------- undo send ---------- */
    // Undo send (AII-202): the message waits here for 5 or 10 seconds; leaving the chat sends it at once.
    val snack = remember { SnackbarHostState() }
    var held by remember { mutableStateOf<Held?>(null) }
    fun sendHeld(h: Held) { held = null; send(h.body, h.to, h.files, h.reply) }
    val heldNow by rememberUpdatedState(held)
    DisposableEffect(ref.key) {
        onDispose { heldNow?.let { h -> flushHeld(repo, ref, h) } }   // ChatUndoSend.kt
    }

    /** What is typed in the box, now or (Send later, AII-146) at [at]. */
    fun sendTyped(at: Instant? = null) {
        val rid = replyId.takeIf { ref.kind == Kind.FRIEND }   // AII-324: a friend reply goes by id, not as a quote in the text
        val body = (if (rid != null) text.trim() else Quote.with(quoting, text.trim())).takeIf { text.isNotBlank() } ?: text
        val to = if (ref.kind == Kind.LEAD) focus ?: target else null
        if (at == null && Appearance.undoSend && (body.isNotBlank() || pics.isNotEmpty())) {
            held?.let { sendHeld(it) }   // a second Send sends the first at once
            val h = Held(text, quoting, to, pics, rid); held = h
            scope.launch {
                val r = showUndoBar(snack, res)   // ChatUndoSend.kt
                if (held !== h) return@launch
                if (r == SnackbarResult.ActionPerformed) {
                    held = null
                    if (text.isBlank() && pics.isEmpty() && quoting == null) { text = h.text; pics = h.files; quoting = h.quote; replyId = h.reply }
                    else { send(h.body, h.to, h.files, h.reply) }   // the box was used meanwhile: never lose either
                } else sendHeld(h)
            }
        }
        else if (at == null) send(body, to, pics, rid)
        else if (body.isNotBlank() || pics.isNotEmpty()) {
            repo.later(ref, body.trim(), to, pics, at, rid); pending = repo.pendingFor(ref.key)
            scope.launch { list.animateScrollToItem(0) }
        }
        text = ""; pics = emptyList(); picNote = null; quoting = null; replyId = null
    }

    /* ---------- pictures ---------- */
    // Pictures (AII-27) and a copied picture (AII-56): ChatPictures.kt
    val picker = rememberPicPicker(repo, { pics }, { pics = it }, { picNote = it })
    val copied = rememberCopiedPicture()
    fun pasteCopied() = pasteCopiedPicture(copied, repo, scope, res, { pics }, { pics = it }, { picNote = it })

    forwarding?.let { body -> ForwardHost(scope, repo, ref, body, { forwarding = null }, { pending = repo.pendingFor(ref.key) }, ::load) }   // ChatParts.kt

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ref.key) {
        // Anything left from last time goes first (it was saved before sending, so nothing is lost).
        waitingToSend(repo.pendingFor(ref.key)).forEach { deliver(it) }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Live: every 3 s while the chat is busy, easing off to 30 s when it is quiet; never in the background.
            while (true) {
                pace.after(load())
                // Send later (AII-146): one that came due while the chat is open goes now (in the demo this is the only way).
                waitingToSend(pending.filter { it.sendAt != null }).forEach { deliver(it) }
                pace.sleep()
            }
        }
    }

    // "working…" under the name while the team is at work (AII-112), "working on AII-5" when we know the card (AII-113):
    // asked of the chats list every 20 s, only on screen.
    var working by remember(ref.key) { mutableStateOf(ref.working) }
    var workingOn by remember(ref.key) { mutableStateOf(ref.workingOn) }
    if (ref.kind == Kind.LEAD && focus == null) LaunchedEffect(ref.key) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { repo.fresh(ref)?.let { working = it.working; workingOn = it.workingOn }; delay(20_000) } }
    }

    val live = rememberLiveRef(repo, ref, text)   // AII-324 (ChatFriend.kt)
    var safetyOk by remember(ref.key) { mutableStateOf(0) }   // AII-333: bumped when you accept new safety words
    val safetyChanged = remember(live.safety, safetyOk) { repo.learnSafety(listOf(live)); repo.safetyChanged(live) }
    val items = remember(convo, pending, owner, focus, clearedAt) { rows(convo, pending, owner, focus, seenAtOpen, clearedAt).asReversed() }
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
    RestoreScroll(list, items, placeKey, place, openAt, seenNewest) { restoring = false }   // AII-72, AII-102 (ChatScroll.kt)
    OpenAtTestEffect(toTest = detail?.status == "verify", list = list, plain = place == null && openAt == null)   // AII-282 (ChatScroll.kt)
    val unread = if (atBottom) 0 else unreadBelow(convo?.bubbles.orEmpty(), seenNewest)
    // AII-204: the next unread chat in chats-list order, worked out as the chats list does it; looked at again whenever you reach the bottom.
    val nextChat = remember(atBottom, convo, Appearance.nextUnread) {
        if (!Appearance.nextUnread) return@remember null   // AII-254
        nextUnreadChat(repo, ref.key)
    }
    // AII-99: tap a quote, the chat scrolls to the quoted message and outlines it for a moment.
    var flash by remember { mutableStateOf<String?>(null) }
    fun jumpToQuote(quoted: String, from: String) {
        val i = Quote.source(quoted, items.indexOfFirst { it.id == from }, items.map { (it as? Row.Msg)?.b }, owner) ?: return
        val id = items[i].id
        scope.launch { list.animateScrollToItem(i + 1); flash = id; delay(1500); if (flash == id) flash = null }   // +1: the spacer at the bottom
    }
    OpenAtEffect(openAt, list, { latestItems }) { flash = it }   // AII-105 (ChatScroll.kt)

    /* ---------- jumps ---------- */
    // AII-138: tap a starred message in the details, the chat scrolls to it and outlines it.
    fun jumpToStar(key: String) {
        val i = items.indexOfFirst { it.id == key }
        if (i < 0) { Toast.makeText(view.context, R.string.star_not_loaded, Toast.LENGTH_SHORT).show(); return }
        scope.launch { list.animateScrollToItem(i + 1); flash = key; delay(1500); if (flash == key) flash = null }   // +1: the spacer at the bottom
    }

    // AII-242: My last message scrolls to your newest sent message (the list is newest first), same jump as a starred one.
    fun jumpToMine() {
        val m = items.firstOrNull { it is Row.Msg && it.b.mine }
        if (m == null) Toast.makeText(view.context, R.string.no_my_message, Toast.LENGTH_SHORT).show() else jumpToStar(m.id)
    }

    androidx.compose.runtime.LaunchedEffect(details) { reminders = repo.store.reminders() }   // a reminder cancelled in the details
    if (details) ChatDetails(live, repo, chatInfo(convo), onChecked = if (ref.kind == Kind.FRIEND) { { scope.launch { runCatching { repo.acceptSafety(live) }.onFailure { error = it.message }; safetyOk++ } } } else null,
 onFind = { details = false; finding = true },
        starred = Stars.inChat(stars, ref.key), onStarred = { details = false; jumpToStar(it) }, onReminder = { details = false; jumpToStar(it) },
        onClear = convo?.takeIf { it.bubbles.isNotEmpty() }?.let { { details = false; clearAsk = true } },
        onShare = convo?.takeIf { it.bubbles.isNotEmpty() }?.let { c -> { shareTranscript(view.context, ref.name, c, owner) } },   // AII-139
        onSave = convo?.takeIf { it.bubbles.isNotEmpty() }?.let { c -> { saveTranscript(view.context, ref.name, c, owner) } }) { details = false; muted = ref.key in repo.store.muted() }   // AII-229
    // AII-363: Clear chat for me asks first, hides everything so far on this phone, and offers Undo for a few seconds.
    if (clearAsk) ClearChatDialog(onDismiss = { clearAsk = false }, onConfirm = {
        clearAsk = false
        val before = clearedAt
        val mark = com.smizflark.aigile.messenger.data.Cleared.mark(convo?.bubbles.orEmpty(), com.smizflark.aigile.messenger.data.Now.instant())
        clearedAt = mark; repo.store.setCleared(com.smizflark.aigile.messenger.data.Cleared.with(repo.store.cleared(), ref.key, mark))
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            if (snack.showSnackbar(res.getString(R.string.chat_cleared), res.getString(R.string.undo), duration = androidx.compose.material3.SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                clearedAt = before; repo.store.setCleared(com.smizflark.aigile.messenger.data.Cleared.with(repo.store.cleared(), ref.key, before))
            }
        }
    })
    if (muteAsk) MuteFor(onPick = { f -> repo.store.setMuted(ref.key, true, com.smizflark.aigile.messenger.data.Mute.until(com.smizflark.aigile.messenger.data.Now.millis(), f)); muted = true; muteAsk = false },
        onDismiss = { muteAsk = false })
    // AII-193: Go to date lands on that day's day row, else the next later day with messages, else the newest.
    var goToDate by remember { mutableStateOf(false) }
    if (goToDate) GoToDateHost(items, list, scope) { goToDate = false }   // ChatScroll.kt
    Scaffold(
        snackbarHost = { SnackbarHost(snack, Modifier.testTag("snack")) },
        topBar = {
            ChatTopBar(ref, repo, focus, focusBubble, working, workingOn, muted, onBack = onBack, onDetails = { details = true },
                onFind = { finding = true }, onMute = { if (muted) { muted = false; repo.store.setMuted(ref.key, false) } else muteAsk = true },
                onGoToDate = { goToDate = true }, onMyLast = { jumpToMine() }, onOpenCard = onOpenCard, silent = silent,
                onSilent = { repo.store.silent = com.smizflark.aigile.messenger.data.Silent.toggle(repo.store.silent, ref.key); silent = ref.key in repo.store.silent },
                typing = live.typing, onClear = { clearAsk = true })
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(10.dp).testTag("error"))
            }
            // AII-339: the pinned bar; tap jumps to the message and the next tap shows the next pin.
            val chatPins = PinnedMsgs.inChat(pins, ref.key)
            if (chatPins.isNotEmpty()) {
                val sh = PinnedMsgs.clamp(pinShown, chatPins.size); val p = chatPins[sh]
                PinnedBar(p.text, sh, chatPins.size, onTap = { jumpToStar(p.key); pinShown = PinnedMsgs.next(sh, chatPins.size) },
                    onUnpin = { pins = PinnedMsgs.remove(pins, ref.key, p.key); repo.store.setPinnedMsgs(pins) })
            }
            if (finding) FindBar(query, { query = it }, hits.size, current,
                onUp = { if (hits.isNotEmpty()) at = (current + 1) % hits.size },   // older
                onDown = { if (hits.isNotEmpty()) at = (current - 1 + hits.size) % hits.size },   // newer
                onClose = { finding = false; query = "" })
            Box(Modifier.weight(1f).fillMaxWidth().pinchTextSize(repo)) {   // AII-259 (ChatGestures.kt)
                LazyColumn(Modifier.fillMaxSize(), state = list, reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Spacer(Modifier.size(if (nextChat != null && convo != null && !finding) 64.dp else 8.dp)) }   // room for the Next unread button (AII-204)
                    items(items, key = { it.id }) { r ->
                        when (r) {
                            is Row.Day -> DayRow(r.label)
                            is Row.New -> NewMessagesLine()
                            is Row.Card -> CardHeader(r, chosen = target == r.card && focus == null,
                                onOpen = if (focus == null && ref.kind == Kind.LEAD) { { onOpenCard(r.card) } } else null) {
                                if (targets.any { it.id == r.card }) { target = r.card; picked = true }
                            }
                            is Row.Msg -> MessageBubble(r, ref, repo, mark = if (r.id == flash) 2 else if (r.id !in hitIds) 0 else if (hitIds.getOrNull(current) == r.id) 2 else 1, onReply = { q ->
                                quoting = Quote.of(if (q.mine) owner else q.author, q.text, q.pics.size)
                                replyId = q.id.takeIf { ref.kind == Kind.FRIEND }   // AII-324
                                // A reply to a team message goes to its card, like answering it on the board.
                                if (ref.kind == Kind.LEAD && focus == null && q.card != null && targets.any { it.id == q.card }) { target = q.card; picked = true }
                            }, onForward = { forwarding = it.text }, cards = if (ref.kind == Kind.LEAD) known - setOfNotNull(focus) else emptySet(), onCard = onOpenCard,
                                onOption = { send(it, r.b.card) },
                                onRetry = { p -> deliver(p) },
                                onSendNow = { p -> pending = repo.pendingFor(ref.key); scope.launch { repo.sendNow(ref, p).onSuccess { load() }.onFailure { error = it.message }; pending = repo.pendingFor(ref.key) } },
                                onDiscard = { p -> repo.discard(p.id); pending = repo.pendingFor(ref.key) },
                                onMoveLater = { p, at -> repo.moveLater(p.id, at); pending = repo.pendingFor(ref.key) },
                                onRepeat = { p, r -> repo.setRepeat(p.id, r); pending = repo.pendingFor(ref.key) },
                                onEdit = { p ->   // AII-150: back in the box, with its card and pictures
                                    repo.takeBack(p.id); pending = repo.pendingFor(ref.key)
                                    text = Outbox.editBack(p, text); pics = p.files + pics
                                    if (p.target != null && targets.any { it.id == p.target }) { target = p.target; picked = true }
                                },
                                onQuote = { q -> jumpToQuote(q, r.id) },
                                onEditMsg = if (ref.kind == Kind.FRIEND) { { b, t -> friendEdit(scope, repo, ref, b, t, { error = it }, ::load) } } else null,   // AII-324
                                onReact = if (ref.kind == Kind.FRIEND) { { b, e -> friendReact(scope, repo, ref, b, e, { error = it }, ::load) } } else null,   // AII-326
                                onDeleteMsg = if (ref.kind == Kind.FRIEND) { { b -> friendDelete(scope, repo, ref, b, { error = it }, ::load) } } else null,
                                remindAt = com.smizflark.aigile.messenger.data.Remind.forMessage(reminders, ref.key, r.b.key), onReminded = { reminders = repo.store.reminders() },
                                starred = Stars.has(stars, ref.key, r.b.key),
                                onStar = if (r.pending == null) { {
                                    val s = Stars.of(ref.key, r.b); val on = !Stars.has(stars, s.chat, s.key)
                                    stars = Stars.toggle(stars, s); scope.launch { repo.setStar(ref, s, on) }   // AII-141: on the board too
                                } } else null,
                                pinned = PinnedMsgs.has(pins, ref.key, r.b.key),
                                onPin = if (r.pending == null) { {
                                    pins = PinnedMsgs.toggle(pins, PinnedMsgs.of(ref.key, r.b, com.smizflark.aigile.messenger.data.Now.millis())); repo.store.setPinnedMsgs(pins)
                                } } else null)
                        }
                    }
                    if (convo == null && error == null) item { Text(stringResource(R.string.loading), Modifier.padding(24.dp)) }
                    detail?.let { d -> item(key = "card-details") { CardDetails(d, ref, repo) { detail = it } } }   // last item = top of the reversed list
                    if (convo != null && convo!!.bubbles.isEmpty() && pending.isEmpty()) item {
                        Text(stringResource(R.string.chat_empty), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DayPill(if (scrolling) topDay else null, onClick = { goToDate = true })
                JumpToNewest(show = !atBottom && convo != null, unread = unread) { scope.launch { list.animateScrollToItem(0) } }
                val next = if (list.firstVisibleItemIndex == 0 && convo != null && !finding) nextChat else null   // AII-204; only once its room at the end shows (AII-273: never over a message)
                NextUnreadButton(next?.name) { next?.let(onOpenChat) }
            }
            // AII-333: the friend's safety code changed: no message box until you have read the new words to each other.
            if (safetyChanged) SafetyChangedBar(ref.name, onShow = { details = true },
                onChecked = { scope.launch { runCatching { repo.acceptSafety(live) }.onFailure { error = it.message }; safetyOk++ } })
            else Composer(ref, text,{ text = it }, targets.map { it.id to it.label }, target, { target = it; picked = true },
                pics = pics, picNote = picNote,
                onAttach = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemovePic = { f -> pics = pics - f; File(f).delete() },
                onPasteCopied = if (copied.available && pics.size < MAX_PICS) ::pasteCopied else null,
                quote = quoting, onClearQuote = { quoting = null; replyId = null },
                onSendLater = { at -> sendTyped(at) }, onEditReplies = onEditReplies, voice = voice,
            ) { sendTyped() }
        }
    }
}
