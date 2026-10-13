package com.smizflark.aigile.messenger.data

import java.text.Normalizer

/** One search result: a chat whose name matches (bubble null), or one message in a chat. */
data class Hit(val ref: ChatRef, val bubble: Bubble? = null, val snippet: String = "")

/** Search across all chats (AII-30): chat names first, then messages newest first. Ignores case and accents. */
object Search {
    const val MAX_HITS = 100
    private const val AROUND = 40

    fun run(query: String, chats: List<ChatRef>, conversations: List<Conversation>): List<Hit> {
        val q = fold(query.trim())
        if (q.length < 2) return emptyList()
        val byName = chats.filter { fold(it.name).contains(q) }.map { Hit(it) }
        val known = chats.associateBy { it.key }
        val inMessages = conversations.flatMap { c ->
            val ref = known[c.ref.key] ?: c.ref
            c.bubbles.mapNotNull { b ->
                val hay = listOfNotNull(b.text, b.cardTitle, b.author).joinToString("\n")
                if (fold(hay).contains(q)) Hit(ref, b, snippet(b.text, q).ifEmpty { b.cardTitle ?: b.author }) else null
            }
        }.sortedByDescending { it.bubble?.at ?: "" }
        return (byName + inMessages).take(MAX_HITS)
    }

    /** The text around the first match, on one line, with "…" where it was cut. */
    fun snippet(text: String, foldedQuery: String): String {
        val flat = text.replace(Regex("\\s+"), " ").trim()
        val at = fold(flat).indexOf(foldedQuery)
        if (at < 0) return flat.take(AROUND * 2)
        val from = (at - AROUND).coerceAtLeast(0)
        val to = (at + foldedQuery.length + AROUND).coerceAtMost(flat.length)
        return (if (from > 0) "…" else "") + flat.substring(from, to).trim() + (if (to < flat.length) "…" else "")
    }

    /** Lower case without accents, keeping one character per character so positions still line up. */
    fun fold(s: String): String = buildString(s.length) {
        for (ch in s) {
            val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).firstOrNull() ?: ch
            append(base.lowercaseChar())
        }
    }
}
