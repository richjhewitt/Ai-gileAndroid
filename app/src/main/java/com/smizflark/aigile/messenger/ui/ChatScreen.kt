package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.SendState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.platform.LocalView

private sealed interface Row {
    val id: String
    data class Day(override val id: String, val label: String) : Row
    data class Card(override val id: String, val card: String, val title: String, val waiting: Boolean) : Row
    data class Msg(override val id: String, val b: Bubble, val pending: Pending? = null, val showOptions: Boolean = false) : Row
}

/** Bubbles, pending messages, day lines and card headers, oldest first. */
private fun rows(c: Conversation?, pending: List<Pending>, owner: String): List<Row> {
    val out = mutableListOf<Row>()
    val bubbles = c?.bubbles.orEmpty()
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
    pending.forEach { p ->
        out += Row.Msg("p:${p.id}", Bubble("p:${p.id}", owner, p.text, p.at, true, p.target,
            state = if (p.failed) SendState.FAILED else SendState.SENDING), p)
    }
    return out
}

/** Journey 3: read a chat and reply. Replies go to the question waiting on you, a card you pick, or a new 💬. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repo: Repo, ref: ChatRef, onBack: () -> Unit) {
    var convo by remember { mutableStateOf(repo.store.cachedConversation(ref.key)) }
    var pending by remember { mutableStateOf(repo.pendingFor(ref.key)) }
    var target by rememberSaveable { mutableStateOf(convo?.defaultTarget) }
    var picked by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    // AII-49: a little tick on send and when a reply lands. performHapticFeedback follows the phone's touch feedback setting.
    val view = LocalView.current
    fun tick() = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    val owner = repo.store.owner.ifBlank { stringResource(R.string.you) }

    suspend fun load() {
        try {
            val c = repo.conversation(ref)
            val before = convo?.bubbles?.lastOrNull { !it.mine }?.at
            val now = c.bubbles.lastOrNull { !it.mine }?.at
            if (before != null && now != null && now > before) tick()
            convo = c; error = null
            if (!picked) target = c.defaultTarget
            repo.store.markSeen(ref.key, c.bubbles.lastOrNull()?.at)
        } catch (e: Exception) { error = e.message }
    }

    fun deliver(p: Pending) {
        pending = repo.pendingFor(ref.key)
        scope.launch {
            val r = repo.deliver(ref, p)
            r.onSuccess { card ->
                // A new 💬 carries on in its own card, like the board's page (AIG-757).
                if (ref.kind == Kind.LEAD && p.target == null && card != null) { target = card; picked = true }
                load()
            }.onFailure { error = it.message }
            pending = repo.pendingFor(ref.key)
            list.animateScrollToItem(0)
        }
    }

    fun send(body: String, to: String?) {
        val t = body.trim(); if (t.isEmpty()) return
        tick()
        deliver(repo.queue(ref, t, to))
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ref.key) {
        // Anything left from last time goes first (it was saved before sending, so nothing is lost).
        repo.pendingFor(ref.key).filter { !it.failed }.forEach { deliver(it) }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { load(); delay(6_000) }
        }
    }

    val items = rows(convo, pending, owner).asReversed()
    val targets = convo?.targets.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(ref, repo, 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(ref.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ref.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
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
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Spacer(Modifier.size(8.dp)) }
                items(items, key = { it.id }) { r ->
                    when (r) {
                        is Row.Day -> Text(r.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        is Row.Card -> CardHeader(r, chosen = target == r.card) {
                            if (targets.any { it.id == r.card }) { target = r.card; picked = true }
                        }
                        is Row.Msg -> MessageBubble(r, ref,
                            onOption = { send(it, r.b.card) },
                            onRetry = { p -> deliver(p) },
                            onDiscard = { p -> repo.discard(p.id); pending = repo.pendingFor(ref.key) })
                    }
                }
                if (convo == null && error == null) item { Text(stringResource(R.string.loading), Modifier.padding(24.dp)) }
                if (convo != null && convo!!.bubbles.isEmpty() && pending.isEmpty()) item {
                    Text(stringResource(R.string.chat_empty), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Composer(ref, text, { text = it }, targets.map { it.id to it.label }, target, { target = it; picked = true }) {
                send(text, if (ref.kind == Kind.LEAD) target else null); text = ""
            }
        }
    }
}

@Composable
private fun CardHeader(r: Row.Card, chosen: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        Surface(
            onClick = onClick, shape = RoundedCornerShape(50),
            color = if (r.waiting) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                (if (r.waiting) "⏳ " else "") + "${r.card}  ${r.title}" + if (chosen) "  ✓" else "",
                style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 300.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(r: Row.Msg, ref: ChatRef, onOption: (String) -> Unit, onRetry: (Pending) -> Unit, onDiscard: (Pending) -> Unit) {
    val b = r.b
    val cs = MaterialTheme.colorScheme
    val bg = when {
        b.unverified -> cs.errorContainer
        b.mine -> cs.primaryContainer
        else -> cs.surfaceContainerHigh
    }
    val fg = if (b.mine) cs.onPrimaryContainer else cs.onSurface
    val shown = Markdown.render(b.text, cs.surfaceVariant, cs.primary)
    // AII-43: long-press a bubble to copy what it shows (without the ** marks).
    var menu by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }   // AII-51: tap a bubble for its exact time
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalAlignment = if (b.mine) Alignment.End else Alignment.Start) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(18.dp, 18.dp, if (b.mine) 4.dp else 18.dp, if (b.mine) 18.dp else 4.dp),
            modifier = Modifier.widthIn(max = 320.dp).testTag(if (b.mine) "mine" else "theirs")
                .combinedClickable(onClick = { showTime = !showTime }, onLongClick = { menu = true }),
        ) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
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
                Text(shown, style = MaterialTheme.typography.bodyLarge, color = fg)
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
        if (showTime && b.at.isNotBlank()) Text(fullTime(b.at), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("fulltime"))
        val p = r.pending
        if (p != null && b.state == SendState.FAILED) Row {
            TextButton(onClick = { onRetry(p) }) { Text(stringResource(R.string.retry)) }
            TextButton(onClick = { onDiscard(p) }) { Text(stringResource(R.string.delete)) }
        }
        if (r.showOptions) FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            b.options.forEach { o -> OutlinedButton(onClick = { onOption(o) }) { Text(o) } }
        }
    }
}

@Composable
private fun Composer(
    ref: ChatRef, text: String, onText: (String) -> Unit,
    targets: List<Pair<String?, String>>, target: String?, onTarget: (String?) -> Unit, onSend: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (ref.kind == Kind.LEAD && targets.isNotEmpty()) {
                var open by remember { mutableStateOf(false) }
                val label = targets.firstOrNull { it.first == target }?.second ?: stringResource(R.string.new_message)
                Box {
                    AssistChip(
                        onClick = { open = true },
                        label = { Text(stringResource(R.string.replying_to, label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.testTag("target"),
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        targets.forEach { (id, l) ->
                            DropdownMenuItem(text = { Text(if (id == null) "✏️ $l" else l, maxLines = 2) }, onClick = { onTarget(id); open = false })
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text, onValueChange = onText, maxLines = 6,
                    placeholder = { Text(if (ref.kind == Kind.WATCHMAN) stringResource(R.string.message_watchman) else stringResource(R.string.message)) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("input"),
                )
                Spacer(Modifier.size(8.dp))
                FilledIconButton(onClick = onSend, enabled = text.isNotBlank(), modifier = Modifier.size(52.dp).testTag("send")) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send))
                }
            }
        }
    }
}
