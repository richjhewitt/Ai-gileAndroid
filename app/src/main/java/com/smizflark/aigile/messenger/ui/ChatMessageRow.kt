package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.data.Kind
import androidx.compose.runtime.Composable
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.PinnedMsg
import com.smizflark.aigile.messenger.data.PinnedMsgs
import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.Reminder
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.SendState
import com.smizflark.aigile.messenger.data.Selection
import com.smizflark.aigile.messenger.data.Star
import com.smizflark.aigile.messenger.data.Stars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* ---------- one message row of the chat list ---------- */

/**
 * One message bubble with everything it does: send now / discard / move / repeat for a waiting one, the friend edit / react / delete
 * (AII-324, AII-326), star, pin, remind and select (AII-371). The screen state stays in ChatScreen and comes in through the
 * getters and setters ([stars], [pins], [selPicked] are read when you tap, so two quick taps both count).
 */
@Composable
internal fun ChatMessageRow(r: Row.Msg, ref: ChatRef, repo: Repo, scope: CoroutineScope, colour: String, mark: Int, cards: Set<String>,
                            onReply: (Bubble) -> Unit, onQuotePart: (Bubble, String) -> Unit, onForward: (Bubble) -> Unit, onCard: (String) -> Unit,
                            onOption: (String) -> Unit, onRetry: (Pending) -> Unit, onEdit: (Pending) -> Unit, onQuote: (String) -> Unit,
                            load: suspend () -> Unit, onError: (String?) -> Unit, onPending: () -> Unit,
                            reminders: List<Reminder>, onReminders: () -> Unit,
                            stars: () -> List<Star>, setStars: (List<Star>) -> Unit,
                            pins: () -> List<PinnedMsg>, setPins: (List<PinnedMsg>) -> Unit,
                            selectMode: Boolean, selPicked: () -> Set<String>, setPicked: (Set<String>) -> Unit, setSelectMode: (Boolean) -> Unit) {
    val friend = ref.kind == Kind.FRIEND
    MessageBubble(r, ref, repo, chatColour = colour, mark = mark, onReply = onReply,
        onQuotePart = onQuotePart, onForward = onForward, cards = cards, onCard = onCard,
        onOption = onOption,
        onRetry = onRetry,
        onSendNow = { p -> onPending(); scope.launch { repo.sendNow(ref, p).onSuccess { load() }.onFailure { onError(it.message) }; onPending() } },
        onDiscard = { p -> repo.discard(p.id); onPending() },
        onMoveLater = { p, at -> repo.moveLater(p.id, at); onPending() },
        onRepeat = { p, r -> repo.setRepeat(p.id, r); onPending() },
        onEdit = onEdit,
        onQuote = onQuote,
        onEditMsg = if (friend) { { b, t -> friendEdit(scope, repo, ref, b, t, onError, load) } } else null,   // AII-324
        onReact = if (friend) { { b, e -> friendReact(scope, repo, ref, b, e, onError, load) } } else null,   // AII-326
        onDeleteMsg = if (friend) { { b -> friendDelete(scope, repo, ref, b, onError, load) } } else null,
        remindAt = Remind.forMessage(reminders, ref.key, r.b.key), onReminded = onReminders,
        starred = Stars.has(stars(), ref.key, r.b.key),
        onStar = if (r.pending == null) { {
            val s = Stars.of(ref.key, r.b); val on = !Stars.has(stars(), s.chat, s.key)
            setStars(Stars.toggle(stars(), s)); scope.launch { repo.setStar(ref, s, on) }   // AII-141: on the board too
        } } else null,
        selectMode = selectMode, picked = r.b.key in selPicked(),
        onToggle = if (r.pending == null) { { val now = Selection.toggle(selPicked(), r.b.key); setPicked(now); if (now.isEmpty()) setSelectMode(false) } } else ({}),
        onPick = if (r.pending == null && r.b.state == SendState.SENT && !r.b.unverified) { { setSelectMode(true); setPicked(setOf(r.b.key)) } } else null,
        pinned = PinnedMsgs.has(pins(), ref.key, r.b.key),
        onPin = if (r.pending == null) { {
            val now = PinnedMsgs.toggle(pins(), PinnedMsgs.of(ref.key, r.b, com.smizflark.aigile.messenger.data.Now.millis())); setPins(now); repo.store.setPinnedMsgs(now)
        } } else null)
}
