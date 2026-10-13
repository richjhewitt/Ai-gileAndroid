package com.smizflark.aigile.messenger.data

/**
 * Forward a message to another chat (AII-71). The board has no "forwarded" field, so the text goes with a first
 * line "↪ Forwarded", then a blank line, then the original. The bubble spots that line and shows it small.
 */
object Forward {
    const val MARK = "↪ Forwarded"

    /** The text to send: the mark, a blank line, then the message (an earlier mark is not stacked). */
    fun text(original: String): String = "$MARK\n\n${split(original).second.trim()}"

    /** A message as (was forwarded, the text without the mark). */
    fun split(text: String): Pair<Boolean, String> {
        val first = text.lineSequence().firstOrNull()?.trim()
        if (first != MARK) return false to text
        return true to text.lines().drop(1).joinToString("\n").trim()
    }

    /** Chats to offer in the picker: those whose name or subtitle contains the search (any case); all when it is blank. */
    fun pickable(chats: List<ChatRef>, query: String): List<ChatRef> {
        val q = query.trim()
        if (q.isEmpty()) return chats
        return chats.filter { it.name.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true) }
    }
}
