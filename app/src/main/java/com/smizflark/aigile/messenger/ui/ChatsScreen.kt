package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatFilter
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Closed
import com.smizflark.aigile.messenger.data.Mute
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pause
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Update
import com.smizflark.aigile.messenger.data.Tips
import com.smizflark.aigile.messenger.data.WhatsNew
import com.smizflark.aigile.messenger.data.pinFirst
import com.smizflark.aigile.messenger.data.mutedLast
import com.smizflark.aigile.messenger.data.togglePin
import com.smizflark.aigile.messenger.work.CheckWorker
import kotlinx.coroutines.launch


/** Journey 2: every chat in one list. Shows the last list at once, then fetches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(repo: Repo, onOpen: (ChatRef) -> Unit, onNew: () -> Unit, onSettings: () -> Unit, onSearch: () -> Unit = {}, onStarred: () -> Unit = {}, onWaiting: () -> Unit = {}, onWhatsNew: () -> Unit = {}, build: Int = BuildConfig.VERSION_CODE) {
    var chats by remember { mutableStateOf(repo.store.cachedChats() ?: emptyList()) }
    var seen by remember { mutableStateOf(repo.store.seen()) }
    fun counts() = chats.associate { it.key to News.unread(it, seen[it.key], repo.store.cachedConversation(it.key)) }
    var unread by remember { mutableStateOf(counts()) }
    var muted by remember { mutableStateOf(repo.store.muted()) }
    var muteAsk by remember { mutableStateOf<ChatRef?>(null) }   // AII-157: the chat whose Mute asks how long
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
        seen = repo.store.seen(); unread = counts(); outbox = repo.store.outbox(); drafts = repo.store.drafts(); closed = repo.store.closed(); muted = repo.store.muted(); loaded = true
        return chats != before
    }

    val context = LocalContext.current
    /** Mark as read (AII-25): one chat from its menu, or all from the top bar. */
    fun markRead(l: List<ChatRef>) = scope.launch {
        l.forEach { repo.markRead(it) }
        seen = repo.store.seen(); unread = counts()
        if (chats.none { News.fresh(it, seen[it.key]) }) CheckWorker.clearAlert(context)
    }
    /** Mark all as read, then a bar with Undo that brings the badges back (AII-158). */
    fun markAllRead() = scope.launch {
        val had = chats.filter { (unread[it.key] ?: 0) > 0 }
        val before = repo.store.seen()
        markRead(chats).join()
        if (had.isEmpty()) return@launch
        snack.currentSnackbarData?.dismiss()
        val r = snack.showSnackbar(context.getString(R.string.all_read), context.getString(R.string.undo), duration = androidx.compose.material3.SnackbarDuration.Short)
        if (r == SnackbarResult.ActionPerformed) { repo.restoreSeen(had, before); seen = repo.store.seen(); unread = counts() }
    }
    /** Mark as unread (AII-119), from a chat's menu. */
    fun markUnread(c: ChatRef) = scope.launch { repo.markUnread(c); seen = repo.store.seen(); unread = counts() }
    val total = com.smizflark.aigile.messenger.data.unreadChatCount(chats, { News.fresh(it, seen[it.key]) }, { it.key in muted })   // AII-50: a dot counts too, like a number
    muteAsk?.let { c ->
        MuteFor(onPick = { f ->
            repo.store.setMuted(c.key, true, Mute.until(Now.millis(), f)); muted = repo.store.muted(); muteAsk = null
            // AII-161: a bar with Undo unmutes it straight away.
            scope.launch {
                snack.currentSnackbarData?.dismiss()
                val r = snack.showSnackbar(context.getString(R.string.muted), context.getString(R.string.undo), duration = androidx.compose.material3.SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) { repo.store.setMuted(c.key, false); muted = repo.store.muted() }
            }
        }, onDismiss = { muteAsk = null })
    }

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

    // AII-268: the first open after an update shows a What's new bar; a brand-new install just stores the version.
    var whatsNew by remember { mutableStateOf(WhatsNew.show(repo.store.lastSeenVersion, build)) }
    LaunchedEffect(Unit) { if (repo.store.lastSeenVersion == 0) repo.store.lastSeenVersion = build }
    fun seeWhatsNew() { repo.store.lastSeenVersion = build; whatsNew = false }

    var tips by remember { mutableStateOf(Tips.show(repo.store.demo, repo.store.tipsSeen)) }   // AII-276

    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // AII-84: one line, so the four buttons never push it onto two; the demo note sits small underneath.
                    // AII-122: tap the title to jump back to the first chat.
                    Column(Modifier.clickable(onClickLabel = stringResource(R.string.to_top)) { scope.launch { list.animateScrollToItem(0) } }.testTag("chats-title")) {
                        Text(if (total > 0) stringResource(R.string.chats_title_unread, total) else stringResource(R.string.chats_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (repo.store.demo) Text(stringResource(R.string.chats_title_demo), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSearch, modifier = Modifier.testTag("open-search")) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
                    if (anyStars) IconButton(onClick = onStarred, modifier = Modifier.testTag("open-starred")) { Icon(Icons.Default.Star, stringResource(R.string.starred)) }
                    if (chats.any { News.fresh(it, seen[it.key]) })
                        IconButton(onClick = { markAllRead() }, Modifier.testTag("readAll")) { Icon(Icons.Default.DoneAll, stringResource(R.string.mark_all_read)) }
                    Box {
                        IconButton(onClick = { pauseMenu = true }, modifier = Modifier.testTag("pause")) { Icon(Icons.Default.NotificationsOff, stringResource(R.string.pause_alerts)) }
                        DropdownMenu(expanded = pauseMenu, onDismissRequest = { pauseMenu = false }) {
                            val now = Now.millis()
                            listOf(R.string.pause_1h to Pause.oneHour(now), R.string.pause_4h to Pause.fourHours(now), R.string.pause_tomorrow to Pause.untilTomorrow(now, hour = Appearance.morning)).forEach { (label, end) ->
                                DropdownMenuItem(text = { Text(stringResource(label, Appearance.morning)) }, onClick = { pauseMenu = false; pausedUntil = end; repo.store.pausedUntil = end })
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
                if (whatsNew) item {
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).testTag("whatsNew")
                        .clickable { seeWhatsNew(); onWhatsNew() }.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.whats_new_bar), Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { seeWhatsNew() }, modifier = Modifier.testTag("whatsNewClose")) {
                            Icon(Icons.Default.Close, stringResource(R.string.whats_new_close), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                if (tips) item {
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).testTag("tips").padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            listOf(R.string.tip_1, R.string.tip_2, R.string.tip_3).forEach {
                                Text(stringResource(it), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        androidx.compose.material3.TextButton(onClick = { repo.store.tipsSeen = true; tips = false }, modifier = Modifier.testTag("tipsGotIt")) {
                            Text(stringResource(R.string.tips_got_it))
                        }
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
                // AII-262: chats with a reminder, for the Reminders chip (and the row marks below).
                val reminded = com.smizflark.aigile.messenger.data.Remind.chatKeys(repo.store.reminders())
                // AII-369: chats with a half-typed reply, for the Drafts chip.
                val drafted = com.smizflark.aigile.messenger.data.Drafts.chatKeys(drafts)
                if (chats.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChatFilter.shown(scheduled.isNotEmpty(), filter, reminded.isNotEmpty(), drafted.isNotEmpty()).forEach { f ->
                            FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(f.res)) },
                                modifier = Modifier.testTag("filter:${f.name.lowercase()}"))
                        }
                    }
                }
                // AII-166: under the Scheduled chip, a link to every waiting message in one list.
                if (filter == ChatFilter.SCHEDULED && scheduled.isNotEmpty()) item {
                    androidx.compose.material3.TextButton(onClick = onWaiting, modifier = Modifier.padding(horizontal = 8.dp).testTag("open-waiting")) {
                        Text(stringResource(R.string.waiting_all))
                    }
                }
                val all = chats.filter { filter.keep(it, News.fresh(it, seen[it.key]), it.key in scheduled, it.key in reminded, it.key in drafted) }
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
                        ChatFilter.REMINDERS -> R.string.empty_filter_reminders
                        ChatFilter.DRAFTS -> R.string.empty_filter_drafts
                        ChatFilter.ALL -> R.string.empty_chats
                    }), button = androidx.compose.ui.res.stringResource(R.string.empty_show_all)) { filter = ChatFilter.ALL }
                }
                if (chats.isEmpty() && error == null) item {
                    if (loaded) EmptyState(androidx.compose.ui.res.stringResource(R.string.empty_chats),
                        button = androidx.compose.ui.res.stringResource(R.string.empty_chats_new), onButton = onNew)
                    else EmptyState(androidx.compose.ui.res.stringResource(R.string.empty_loading))
                }
                val unsent = outbox.groupBy { it.chat }
                val sorted = com.smizflark.aigile.messenger.data.sortChats(shown, Appearance.sortAz)
                val rows = (if (Appearance.mutedLast) mutedLast(sorted, pinned, muted) else pinFirst(sorted, pinned)) + if (showClosed) shut else emptyList()
                items(rows, key = { it.key }) { c ->
                    val isShut = c in shut
                    ChatRow(c, repo, News.fresh(c, seen[c.key]), unread[c.key] ?: 0, c.key in pinned, c.key in muted, c.key in reminded, unsent[c.key].orEmpty(), drafts[c.key], isShut,
                        onRead = { markRead(listOf(c)) }, onUnread = { markUnread(c) },
                        onMute = { on -> if (on) muteAsk = c else { repo.store.setMuted(c.key, false); muted = repo.store.muted() } },
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

