package com.smizflark.aigile.messenger.ui

import androidx.compose.ui.res.stringResource

import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Pause
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Badge
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import com.smizflark.aigile.messenger.data.SwipeAction
import com.smizflark.aigile.messenger.data.swipeAction
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.work.CheckWorker
import com.smizflark.aigile.messenger.data.pinFirst
import com.smizflark.aigile.messenger.data.togglePin
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.saveable.rememberSaveable
import com.smizflark.aigile.messenger.data.ChatFilter
import com.smizflark.aigile.messenger.data.Closed
import com.smizflark.aigile.messenger.data.Update
import com.smizflark.aigile.messenger.BuildConfig
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.delay
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch

/** Journey 2: every chat in one list. Shows the last list at once, then fetches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(repo: Repo, onOpen: (ChatRef) -> Unit, onNew: () -> Unit, onSettings: () -> Unit, onSearch: () -> Unit = {}, onStarred: () -> Unit = {}) {
    var chats by remember { mutableStateOf(repo.store.cachedChats() ?: emptyList()) }
    var seen by remember { mutableStateOf(repo.store.seen()) }
    fun counts() = chats.associate { it.key to News.unread(it, seen[it.key], repo.store.cachedConversation(it.key)) }
    var unread by remember { mutableStateOf(counts()) }
    var muted by remember { mutableStateOf(repo.store.muted()) }
    var outbox by remember { mutableStateOf(repo.store.outbox()) }
    var drafts by remember { mutableStateOf(repo.store.drafts()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(chats.isNotEmpty()) }
    var pinned by remember { mutableStateOf(repo.store.pinned) }
    val anyStars = remember { repo.store.stars().isNotEmpty() }   // AII-140: the Starred button shows once something is starred
    var closed by remember { mutableStateOf(repo.store.closed()) }
    var showClosed by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(ChatFilter.ALL) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var pausedUntil by remember { mutableStateOf(repo.store.pausedUntil) }
    var pauseMenu by remember { mutableStateOf(false) }
    val pace = remember { Pace(fast = 10_000, slow = 60_000) }

    /** Fetches the list; true when a chat changed. */
    suspend fun load(): Boolean {
        val before = chats
        outbox = repo.store.outbox()   // first, so it shows at once even with no signal
        try { chats = repo.chats(); error = null }
        catch (e: Exception) { error = e.message }
        seen = repo.store.seen(); unread = counts(); outbox = repo.store.outbox(); drafts = repo.store.drafts(); closed = repo.store.closed(); loaded = true
        return chats != before
    }

    val context = LocalContext.current
    /** Mark as read (AII-25): one chat from its menu, or all from the top bar. */
    fun markRead(l: List<ChatRef>) = scope.launch {
        l.forEach { repo.markRead(it) }
        seen = repo.store.seen(); unread = counts()
        if (chats.none { News.fresh(it, seen[it.key]) }) CheckWorker.clearAlert(context)
    }
    /** Mark as unread (AII-119), from a chat's menu. */
    fun markUnread(c: ChatRef) = scope.launch { repo.markUnread(c); seen = repo.store.seen(); unread = counts() }
    val total = unread.values.sum()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Calm: every 10 s while chats are busy, easing off to a minute when quiet; never in the background.
            while (true) { pace.after(load()); pace.sleep() }
        }
    }

    // AII-132: ask GitHub for the latest release at most once a day; the bar shows while it is newer than this app.
    var release by remember { mutableStateOf(repo.store.release) }
    LaunchedEffect(Unit) {
        val now = Now.millis()
        if (Update.due(now, repo.store.updateChecked)) {
            repo.store.updateChecked = now
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Update.fetch() }?.let { repo.store.release = it; release = it }
        }
    }

    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // AII-84: one line, so the four buttons never push it onto two; the demo note sits small underneath.
                    // AII-122: tap the title to jump back to the first chat.
                    Column(Modifier.clickable(onClickLabel = stringResource(R.string.to_top)) { scope.launch { list.animateScrollToItem(0) } }.testTag("chats-title")) {
                        Text(stringResource(R.string.chats_title) + if (total > 0) " ($total)" else "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (repo.store.demo) Text(stringResource(R.string.chats_title_demo), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSearch, modifier = Modifier.testTag("open-search")) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
                    if (anyStars) IconButton(onClick = onStarred, modifier = Modifier.testTag("open-starred")) { Icon(Icons.Default.Star, stringResource(R.string.starred)) }
                    if (chats.any { News.fresh(it, seen[it.key]) })
                        IconButton(onClick = { markRead(chats) }, Modifier.testTag("readAll")) { Icon(Icons.Default.DoneAll, stringResource(R.string.mark_all_read)) }
                    Box {
                        IconButton(onClick = { pauseMenu = true }, modifier = Modifier.testTag("pause")) { Icon(Icons.Default.NotificationsOff, stringResource(R.string.pause_alerts)) }
                        DropdownMenu(expanded = pauseMenu, onDismissRequest = { pauseMenu = false }) {
                            val now = Now.millis()
                            listOf(R.string.pause_1h to Pause.oneHour(now), R.string.pause_4h to Pause.fourHours(now), R.string.pause_tomorrow to Pause.untilTomorrow(now)).forEach { (label, end) ->
                                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { pauseMenu = false; pausedUntil = end; repo.store.pausedUntil = end })
                            }
                        }
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, stringResource(R.string.settings)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack, Modifier.testTag("snack")) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                text = { Text(stringResource(R.string.new_message)) }, modifier = Modifier.testTag("new"))
        },
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            LazyColumn(Modifier.fillMaxSize().testTag("chats-list"), state = list) {
                if (Pause.paused(Now.millis(), pausedUntil)) item {
                    val at = java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                        .format(java.time.Instant.ofEpochMilli(pausedUntil).atZone(java.time.ZoneId.systemDefault()))
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(start = 16.dp, end = 8.dp).testTag("pausedBanner"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.paused_until, at), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.TextButton(onClick = { pausedUntil = 0L; repo.store.pausedUntil = 0L }, modifier = Modifier.testTag("resume")) { Text(stringResource(R.string.resume)) }
                    }
                }
                release?.takeIf { Update.newer(it, BuildConfig.VERSION_CODE) }?.let { r ->
                    item {
                        Text(stringResource(R.string.new_version), Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(r.url))) } }
                            .padding(horizontal = 16.dp, vertical = 12.dp).testTag("newVersion"),
                            color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                error?.let { e ->
                    item {
                        Text(
                            if (chats.isEmpty()) e else stringResource(R.string.chats_offline, e, shortTime(java.time.Instant.ofEpochMilli(repo.store.lastSync).toString())),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                        )
                    }
                }
                // AII-152: chats with a Send later message waiting, for the Scheduled chip.
                val scheduled = outbox.filter { com.smizflark.aigile.messenger.work.later(it, Now.instant()) }.map { it.chat }.toSet()
                if (chats.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChatFilter.shown(scheduled.isNotEmpty(), filter).forEach { f ->
                            FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(f.res)) },
                                modifier = Modifier.testTag("filter:${f.name.lowercase()}"))
                        }
                    }
                }
                val all = chats.filter { filter.keep(it, News.fresh(it, seen[it.key]) && (unread[it.key] ?: 0) > 0, it.key in scheduled) }
                // Closed chats (AII-131) wait under a link at the bottom until a newer message brings them back.
                val shut = all.filter { Closed.hidden(it, closed[it.key]) }
                val shown = all - shut.toSet()
                if (chats.isNotEmpty() && all.isEmpty()) item {
                    EmptyState(androidx.compose.ui.res.stringResource(when (filter) {
                        ChatFilter.UNREAD -> R.string.empty_unread
                        ChatFilter.WAITING -> R.string.empty_waiting
                        ChatFilter.TEAMS -> R.string.empty_teams
                        ChatFilter.FRIENDS -> R.string.empty_friends
                        ChatFilter.SCHEDULED -> R.string.empty_scheduled
                        ChatFilter.ALL -> R.string.empty_chats
                    }), button = androidx.compose.ui.res.stringResource(R.string.empty_show_all)) { filter = ChatFilter.ALL }
                }
                if (chats.isEmpty() && error == null) item {
                    if (loaded) EmptyState(androidx.compose.ui.res.stringResource(R.string.empty_chats),
                        button = androidx.compose.ui.res.stringResource(R.string.empty_chats_new), onButton = onNew)
                    else EmptyState(androidx.compose.ui.res.stringResource(R.string.empty_loading))
                }
                val unsent = outbox.groupBy { it.chat }
                val rows = pinFirst(shown, pinned) + if (showClosed) shut else emptyList()
                items(rows, key = { it.key }) { c ->
                    val isShut = c in shut
                    ChatRow(c, repo, News.fresh(c, seen[c.key]), unread[c.key] ?: 0, c.key in pinned, c.key in muted, unsent[c.key].orEmpty(), drafts[c.key], isShut,
                        onRead = { markRead(listOf(c)) }, onUnread = { markUnread(c) },
                        onMute = { on -> repo.store.setMuted(c.key, on); muted = repo.store.muted() },
                        onPin = { pinned = togglePin(pinned, c.key); repo.store.pinned = pinned },
                        onClose = { shutIt -> scope.launch {
                            repo.setClosed(c, shutIt); closed = repo.store.closed()
                            // AII-154: a bar with Undo after Close chat puts it straight back.
                            if (shutIt) {
                                snack.currentSnackbarData?.dismiss()
                                val r = snack.showSnackbar(context.getString(R.string.chat_closed), context.getString(R.string.undo), duration = androidx.compose.material3.SnackbarDuration.Short)
                                if (r == SnackbarResult.ActionPerformed) { repo.setClosed(c, false); closed = repo.store.closed() }
                            }
                        } }) { onOpen(c) }
                    HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                if (shut.isNotEmpty()) item(key = "closed-link") {
                    androidx.compose.material3.TextButton(onClick = { showClosed = !showClosed }, modifier = Modifier.padding(horizontal = 8.dp).testTag("show-closed")) {
                        Text(if (showClosed) stringResource(R.string.closed_hide) else stringResource(R.string.closed_show, shut.size))
                    }
                }
                item { Spacer(Modifier.size(88.dp)) }
            }
        }
    }
}

/** One chat; long-press for Mark as read (or unread, AII-119), Pin to top (AII-25, AII-47), Mute and Close chat (AII-131). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(c: ChatRef, repo: Repo, fresh: Boolean, unread: Int, pinned: Boolean, muted: Boolean, unsent: List<Pending>, draft: String?, closed: Boolean,
                    onRead: () -> Unit, onUnread: () -> Unit, onMute: (Boolean) -> Unit, onPin: () -> Unit, onClose: (Boolean) -> Unit, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    // Swipe right = mark read, left = mute/unmute (AII-61). The row always springs back.
    val mutedNow by rememberUpdatedState(muted)
    val box = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (swipeAction(when (v) { SwipeToDismissBoxValue.StartToEnd -> true; SwipeToDismissBoxValue.EndToStart -> false; else -> null }, mutedNow)) {
            SwipeAction.READ -> onRead()
            SwipeAction.MUTE -> onMute(true)
            SwipeAction.UNMUTE -> onMute(false)
            null -> {}
        }
        false
    })
    SwipeToDismissBox(box, modifier = Modifier.testTag("swipe:${c.key}"), backgroundContent = {
        val toRead = box.dismissDirection == SwipeToDismissBoxValue.StartToEnd
        val idle = box.dismissDirection == SwipeToDismissBoxValue.Settled
        Box(Modifier.fillMaxSize().background(when {
            idle -> Color.Transparent
            toRead -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }).padding(horizontal = 24.dp), contentAlignment = if (toRead) Alignment.CenterStart else Alignment.CenterEnd) {
            if (!idle) Icon(if (toRead) Icons.Default.DoneAll else if (muted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                stringResource(if (toRead) R.string.mark_read else if (muted) R.string.unmute else R.string.mute))
        }
    }) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).combinedClickable(onClickLabel = stringResource(R.string.open_chat), role = Role.Button, onClick = onClick,
            onLongClick = { menu = true }, onLongClickLabel = stringResource(R.string.options))
            .padding(horizontal = 16.dp, vertical = 12.dp).testTag("chat:${c.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
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
        Avatar(c, repo)
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
    if (failed == 0 && held.size == unsent.size) return Words.sendsAt.format(clock(held.minBy { java.time.Instant.parse(it.sendAt) }.sendAt))
    return if (failed > 0) Words.unsentFailed.format(failed) else Words.unsentWaiting.format(unsent.size - held.size)
}

/** The line under a chat's name: "working…" while the team is at work (AII-112), else its newest message ("You: ..." when it is yours, AII-106), else the chat's subtitle. */
fun previewLabel(c: ChatRef): String = if (c.working) Words.working else c.preview?.let { if (c.previewMine) Words.you.format(it) else it } ?: c.subtitle

/** "Draft: ..." for a chat with a half-typed reply (AII-98), on one line; null when there is none. */
fun draftLabel(draft: String?): String? =
    draft?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }?.let { Words.draft.format(it) }
