package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.runtime.mutableStateOf
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
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Quote
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import android.view.HapticFeedbackConstants
import com.smizflark.aigile.messenger.data.ScrollPlaces
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalView

/**
 * Journey 3: read a chat and reply. Replies go to the question waiting on you, a card you pick, or a new 💬.
 * With [focus] (AII-28) it shows one card of the chat: only its messages, and replies go to that card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repo: Repo, listed: ChatRef, onBack: () -> Unit, focus: String? = null, onOpenCard: (String) -> Unit = {}, openAt: String? = null, onOpenChat: (ChatRef) -> Unit = {}, onEditReplies: () -> Unit = {}) {
    var nameRev by remember { mutableStateOf(0) }   // AII-365: bumped when the chat is renamed in its details
    val ref = remember(listed, nameRev) { com.smizflark.aigile.messenger.data.ChatNames.on(listed, repo.store.chatNames) }
    /* ---------- state ---------- */
    var convo by remember { mutableStateOf(repo.store.cachedConversation(ref.key)) }
    val seenAtOpen = rememberSaveable(ref.key) { mutableStateOf(repo.store.seen()[ref.key]) }.value   // AII-100: the "New messages" line stays put while you read
    var pending by remember { mutableStateOf(repo.pendingFor(ref.key)) }
    var target by rememberSaveable { mutableStateOf(focus ?: convo?.defaultTarget) }
    var picked by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf(repo.store.draft(ref.key)) }
    var pics by rememberSaveable { mutableStateOf(listOf<String>()) }
    var editingPic by remember { mutableStateOf<String?>(null) }   // AII-385: the picked picture in the crop and rotate editor
    var picNote by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<CardDetail?>(null) }
    if (focus != null && ref.kind == Kind.LEAD) LaunchedEffect(focus) { detail = repo.card(ref, focus) }
    var quoting by rememberSaveable { mutableStateOf<String?>(null) }   // AII-60: the quoted line of the message being answered
    var replyId by rememberSaveable { mutableStateOf<String?>(null) }   // AII-324: friend chats send the id of the line being answered
    var forwarding by remember { mutableStateOf<String?>(null) }   // AII-71: the text of the message being forwarded
    var error by remember { mutableStateOf<String?>(null) }
    var muted by remember { mutableStateOf(ref.key in repo.store.muted()) }
    var silent by remember { mutableStateOf(ref.key in repo.store.silent) }
    var colour by remember { mutableStateOf(com.smizflark.aigile.messenger.data.ChatColour.of(repo.store.chatColours, ref.key)) }   // AII-375
    var muteAsk by remember { mutableStateOf(false) }   // AII-157: Mute asks how long
    var reminders by remember { mutableStateOf(repo.store.reminders()) }   // AII-251: read once here, not per bubble
    var details by remember { mutableStateOf(false) }   // AII-80: the chat details sheet
    var stars by remember { mutableStateOf(repo.store.stars()) }   // AII-138: starred messages
    var pins by remember { mutableStateOf(repo.store.pinnedMsgs()) }   // AII-339: pinned messages, this phone only
    var clearedAt by remember(ref.key) { mutableStateOf(repo.store.cleared()[ref.key]) }   // AII-363: Clear chat for me, this phone only
    var clearAsk by remember { mutableStateOf(false) }
    var pinShown by remember { mutableStateOf(0) }   // AII-339: which pin the bar shows
    var selPicked by remember(ref.key) { mutableStateOf(emptySet<String>()) }   // AII-371: select several messages; none picked = not selecting
    var selectMode by remember(ref.key) { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = selectMode) { selectMode = false; selPicked = emptySet() }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    var finding by rememberSaveable { mutableStateOf(false) }   // AII-78: the find bar
    var query by rememberSaveable { mutableStateOf("") }
    var at by rememberSaveable { mutableStateOf(0) }   // which match, 0 = the newest
    // AII-49: a little tick on send and when a reply lands. performHapticFeedback follows the phone's touch feedback setting.
    val view = LocalView.current
    fun tick() = Appearance.buzz && view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    val owner = repo.store.owner.ifBlank { "You" }
    val homeCtx = LocalContext.current
    val res = homeCtx.resources

    // Keep the half-typed reply (AII-42): saved a moment after typing stops, and on the way out.
    KeepDraft(repo, ref, text)   // ChatScreenParts.kt
    DisposableEffect(ref.key) { onDispose { ReadAloud.stop(); VoicePlayer.stop() } }   // AII-331: and a playing voice note   // AII-185: leaving the chat stops Read aloud

    val pace = remember { Pace(fast = 3_000, slow = 30_000) }

    /* ---------- loading and sending ---------- */

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

    /* ---------- live refresh ---------- */
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
    if (ref.kind == Kind.LEAD && focus == null) WatchWorking(repo, ref, lifecycle) { working = it.working; workingOn = it.workingOn }   // ChatScreenParts.kt

    /* ---------- rows, find and scrolling ---------- */
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
    val topDay by rememberFloatingDay(list, items)   // ChatScreenParts.kt
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
    // Reply (whole line) or Quote (picked words, AII-409): same reply id, same card as answering it on the board.
    fun startReply(q: Bubble, quote: String) {
        quoting = quote
        replyId = q.id.takeIf { ref.kind == Kind.FRIEND }   // AII-324
        // A reply to a team message goes to its card, like answering it on the board.
        if (ref.kind == Kind.LEAD && focus == null && q.card != null && targets.any { it.id == q.card }) { target = q.card; picked = true }
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
    // AII-138: tap a starred message in the details, the chat scrolls to it and outlines it. AII-242: My last message (ChatMenuActions.kt).
    fun jumpToStar(key: String) = jumpToRow(key, items, list, scope, view.context, { flash }, { flash = it })
    fun jumpToMine() = jumpToMyLast(items, list, scope, view.context, { flash }, { flash = it })

    androidx.compose.runtime.LaunchedEffect(details) { reminders = repo.store.reminders() }   // a reminder cancelled in the details
    if (details) ChatDetailsHost(live, ref, repo, convo, owner, stars, onColour = { colour = it }, onRenamed = { nameRev++ },   // ChatHosts.kt
        onChecked = if (ref.kind == Kind.FRIEND) { { scope.launch { runCatching { repo.acceptSafety(live) }.onFailure { error = it.message }; safetyOk++ } } } else null,
        onFind = { details = false; finding = true }, onJump = { details = false; jumpToStar(it) }, onClear = { details = false; clearAsk = true },
        onClose = { details = false; muted = ref.key in repo.store.muted() })
    // AII-363: Clear chat for me asks first, hides everything so far on this phone, and offers Undo for a few seconds.
    if (clearAsk) ClearChatHost(repo, ref, convo, clearedAt, snack, scope, onDismiss = { clearAsk = false }, onCleared = { clearedAt = it })
    if (muteAsk) MuteFor(onPick = { f -> muteChatFor(repo, ref, f); muted = true; muteAsk = false },
        onDismiss = { muteAsk = false })
    // AII-193: Go to date lands on that day's day row, else the next later day with messages, else the newest.
    var goToDate by remember { mutableStateOf(false) }
    if (goToDate) GoToDateHost(items, list, scope) { goToDate = false }   // ChatScroll.kt
    /* ---------- select several messages (AII-371) ---------- */
    fun leaveSelect() { selectMode = false; selPicked = emptySet() }
    /* ---------- screen ---------- */
    Scaffold(
        snackbarHost = { SnackbarHost(snack, Modifier.testTag("snack")) },
        topBar = {
            if (selectMode) ChatSelectBar(ref, repo, items, selPicked, stars, owner, scope, onStars = { stars = it }, onLeave = { leaveSelect() })   // ChatSelect.kt
            else ChatTopBar(ref, repo, focus, focusBubble, working, workingOn, muted, onBack = onBack, onDetails = { details = true },
                onFind = { finding = true }, onMute = { if (muted) { muted = false; repo.store.setMuted(ref.key, false) } else muteAsk = true },
                onGoToDate = { goToDate = true }, onMyLast = { jumpToMine() }, onOpenCard = onOpenCard, silent = silent,
                onAddHome = { addChatToHome(homeCtx, ref) },   // ChatMenuActions.kt
                onSilent = { silent = toggleChatSilent(repo, ref) },
                typing = live.typing, onClear = { clearAsk = true }, chatColour = colour)
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
            error?.let { ErrorLine(it) }
            ChatPinnedBar(repo, ref, convo, pins, clearedAt, pinShown, { pinShown = it }, { jumpToStar(it) }, { pins = it })   // AII-339 (ChatHosts.kt)
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
                            is Row.Msg -> ChatMessageRow(r, ref, repo, scope, colour, mark = if (r.id == flash) 2 else if (r.id !in hitIds) 0 else if (hitIds.getOrNull(current) == r.id) 2 else 1,   // ChatMessageRow.kt
                                cards = if (ref.kind == Kind.LEAD) known - setOfNotNull(focus) else emptySet(),
                                onReply = { q -> startReply(q, Quote.of(if (q.mine) owner else q.author, q.text, q.pics.size)) },
                                onQuotePart = { q, words -> startReply(q, Quote.part(if (q.mine) owner else q.author, words)) }, onForward = { forwarding = it.text }, onCard = onOpenCard,
                                onOption = { send(it, r.b.card) },
                                onRetry = { p -> deliver(p) },
                                onEdit = { p ->   // AII-150: back in the box, with its card and pictures
                                    repo.takeBack(p.id); pending = repo.pendingFor(ref.key)
                                    text = Outbox.editBack(p, text); pics = p.files + pics
                                    if (p.target != null && targets.any { it.id == p.target }) { target = p.target; picked = true }
                                },
                                onQuote = { q -> jumpToQuote(q, r.id) },
                                load = ::load, onError = { error = it }, onPending = { pending = repo.pendingFor(ref.key) },
                                reminders = reminders, onReminders = { reminders = repo.store.reminders() },
                                stars = { stars }, setStars = { stars = it }, pins = { pins }, setPins = { pins = it },
                                selectMode = selectMode, selPicked = { selPicked }, setPicked = { selPicked = it }, setSelectMode = { selectMode = it })
                        }
                    }
                    if (convo == null && error == null) item { Text(stringResource(R.string.loading), Modifier.padding(24.dp)) }
                    detail?.let { d -> item(key = "card-details") { CardDetails(d, ref, repo) { detail = it } } }   // last item = top of the reversed list
                    if (convo != null && convo!!.bubbles.isEmpty() && pending.isEmpty()) item {
                        Text(stringResource(R.string.chat_empty), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DayPill(topDay, onClick = { goToDate = true })
                JumpToNewest(show = !atBottom && convo != null, unread = unread) { scope.launch { list.animateScrollToItem(0) } }
                val next = if (list.firstVisibleItemIndex == 0 && convo != null && !finding) nextChat else null   // AII-204; only once its room at the end shows (AII-273: never over a message)
                NextUnreadButton(next?.name) { next?.let(onOpenChat) }
            }
            editingPic?.let { old ->   // AII-385: Done swaps the picked picture for the edited copy; the phone's original is never touched
                PicEditorDialog(old, onCancel = { editingPic = null }, onDone = { new ->
                    pics = pics.map { if (it == old) new else it }; File(old).delete(); editingPic = null
                })
            }
            // AII-333: the friend's safety code changed: no message box until you have read the new words to each other.
            if (safetyChanged) SafetyChangedBar(ref.name, onShow = { details = true },
                onChecked = { scope.launch { runCatching { repo.acceptSafety(live) }.onFailure { error = it.message }; safetyOk++ } })
            else Composer(ref, text,{ text = it }, targets.map { it.id to it.label }, target, { target = it; picked = true },
                pics = pics, picNote = picNote,
                onAttach = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemovePic = { f -> pics = pics - f; File(f).delete() },
                onEditPic = if (ref.kind == Kind.FRIEND) ({ f -> editingPic = f }) else null,   // AII-385
                onPasteCopied = if (copied.available && pics.size < MAX_PICS) ::pasteCopied else null,
                onPictures = { u, m -> receivePictures(ref.kind, u, m, repo, scope, view.context, { pics }, { pics = it }, { picNote = it }) },
                quote = quoting, onClearQuote = { quoting = null; replyId = null },
                onSendLater = { at -> sendTyped(at) }, onEditReplies = onEditReplies, voice = voice,
            ) { sendTyped() }
        }
    }
}
