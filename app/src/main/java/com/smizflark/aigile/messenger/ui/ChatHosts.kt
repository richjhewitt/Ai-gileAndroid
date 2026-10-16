package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.data.Repo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.PinnedMsg
import com.smizflark.aigile.messenger.data.PinnedMsgs
import com.smizflark.aigile.messenger.data.Star
import com.smizflark.aigile.messenger.data.Stars
import com.smizflark.aigile.messenger.data.chatInfo

/* ---------- pinned bar (AII-339) ---------- */

/** The pinned bar; tap jumps to the message and the next tap shows the next pin. State stays in ChatScreen. */
@Composable
internal fun ChatPinnedBar(repo: Repo, ref: ChatRef, convo: Conversation?, pins: List<PinnedMsg>, clearedAt: String?, shown: Int,
                           onShown: (Int) -> Unit, onJump: (String) -> Unit, onPins: (List<PinnedMsg>) -> Unit) {
    val chatPins = PinnedMsgs.shown(pins, ref.key, convo?.bubbles.orEmpty(), clearedAt)
    if (chatPins.isEmpty()) return
    val sh = PinnedMsgs.clamp(shown, chatPins.size); val p = chatPins[sh]
    val pinColors = MaterialTheme.colorScheme
    PinnedBar(Markdown.render(p.text, pinColors.surfaceVariant, pinColors.primary).text, sh, chatPins.size, onTap = { onJump(p.key); onShown(PinnedMsgs.next(sh, chatPins.size)) },
        onUnpin = { val now = PinnedMsgs.remove(pins, ref.key, p.key); onPins(now); repo.store.setPinnedMsgs(now) })
}

/* ---------- chat details sheet (AII-80) ---------- */

/** The chat details sheet. [onJump] opens a starred message or reminder; [onClear] is null with nothing to clear. */
@Composable
internal fun ChatDetailsHost(live: ChatRef, ref: ChatRef, repo: Repo, convo: Conversation?, owner: String, stars: List<Star>,
                             onColour: (String) -> Unit, onRenamed: () -> Unit, onChecked: (() -> Unit)?, onFind: () -> Unit,
                             onJump: (String) -> Unit, onClear: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    ChatDetails(live, repo, chatInfo(convo), onColour = onColour, onRenamed = onRenamed, onChecked = onChecked,
        onFind = onFind,
        starred = Stars.inChat(stars, ref.key), onStarred = onJump, onReminder = onJump,
        onClear = convo?.takeIf { it.bubbles.isNotEmpty() }?.let { { onClear() } },
        onShare = convo?.takeIf { it.bubbles.isNotEmpty() }?.let { c -> { shareTranscript(ctx, ref.name, c, owner) } },   // AII-139
        onSave = convo?.takeIf { it.bubbles.isNotEmpty() }?.let { c -> { saveTranscript(ctx, ref.name, c, owner) } }, onClose = onClose)   // AII-229
}
