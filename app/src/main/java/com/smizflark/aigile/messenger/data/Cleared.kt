package com.smizflark.aigile.messenger.data

import java.time.Instant

/** Clear chat for me (AII-363): every message up to a moment is hidden on this phone only; the board and the friend keep theirs. Pure, so it is unit tested. */
object Cleared {
    private fun at(iso: String?): Instant? = runCatching { Instant.parse(iso) }.getOrNull()

    /** The moment to clear up to, as an ISO time like the messages use: now, or the newest message's time if the board's clock is ahead. */
    fun mark(bubbles: List<Bubble>, now: Instant): String {
        val newest = bubbles.mapNotNull { at(it.at) }.maxOrNull()
        return (if (newest != null && newest > now) newest else now).toString()
    }

    /** Only the messages newer than [mark]; all of them when the chat was never cleared. Messages with an unreadable time stay. */
    fun visible(bubbles: List<Bubble>, mark: String?): List<Bubble> {
        val cut = at(mark) ?: return bubbles
        return bubbles.filter { b -> at(b.at)?.let { it > cut } ?: true }
    }

    /** AII-405: what Search and the widgets may use: cleared messages hidden, deleted ones never. */
    fun usable(bubbles: List<Bubble>, mark: String?): List<Bubble> = visible(bubbles, mark).filter { !it.deleted }

    /** AII-405: a chat row whose newest message was cleared shows no preview line (the time stays). */
    fun row(ref: ChatRef, mark: String?): ChatRef {
        val cut = at(mark) ?: return ref
        val last = at(ref.last) ?: return ref
        return if (last > cut) ref else ref.copy(preview = null, previewMine = false, subtitle = "")
    }

    /** The saved marks with this chat's mark set, or taken away when [mark] is null (Undo of a first clear). */
    fun with(all: Map<String, String>, chat: String, mark: String?): Map<String, String> =
        if (mark == null) all - chat else all + (chat to mark)
}
