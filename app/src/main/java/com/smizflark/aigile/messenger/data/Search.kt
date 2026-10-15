package com.smizflark.aigile.messenger.data

import java.text.Normalizer

/** One search result: a chat whose name matches (bubble null), or one message in a chat. */
data class Hit(val ref: ChatRef, val bubble: Bubble? = null, val snippet: String = "", val marks: List<IntRange> = emptyList())

/** Search across all chats (AII-30): chat names first, then messages newest first. Ignores case and accents. */
object Search {
    const val MAX_HITS = 100
    private const val AROUND = 40

    /** AII-192: a web address, with http(s):// or bare like dnbloco.com (the same idea as Markdown's). */
    private val link = Regex("""https?://\S+|(?<![\w@/.:"=-])((?i:(?:[a-z0-9-]+\.)+(?:com|net|org|io|live|dev|app|co|uk|nl|be|ai|me|tv|gg|xyz|info|site|online))(?::\d+)?)(?![\w-])""")

    fun hasLink(text: String) = link.containsMatchIn(text)
    fun hasPicture(b: Bubble) = b.pics.any { it.image } || Gif.inText(b.text).isNotEmpty()   // a GIF link shows as a picture too (AII-188)

    fun hasFile(b: Bubble) = b.pics.any { isFile(it) }   // AII-267: same detection as the chat info Files list

    /**
     * [pictures] / [links] / [files] (AII-192, AII-267): only messages with a picture / a web link / a non-picture file; chats are left out, and an empty query
     * lists every match newest first.
     */
    fun run(query: String, chats: List<ChatRef>, conversations: List<Conversation>, pictures: Boolean = false, links: Boolean = false, files: Boolean = false): List<Hit> {
        val q = fold(query.trim())
        val filtered = pictures || links || files
        if (q.length < 2 && !filtered) return emptyList()
        val typed = q.length >= 2
        val byName = if (filtered) emptyList() else chats.filter { fold(it.name).contains(q) }.map { Hit(it) }
        val known = chats.associateBy { it.key }
        val inMessages = conversations.flatMap { c ->
            val ref = known[c.ref.key] ?: c.ref
            c.bubbles.mapNotNull { b ->
                val hay = listOfNotNull(b.text, b.cardTitle, b.author).joinToString("\n")
                val ok = (!pictures || hasPicture(b)) && (!links || hasLink(b.text)) && (!files || hasFile(b)) && (!typed || fold(hay).contains(q))
                if (!ok) return@mapNotNull null
                val snip = if (typed) snippet(b.text, q) else tidy(b.text).replace(Regex("\\s+"), " ").trim().take(AROUND * 2)
                if (snip.isEmpty()) Hit(ref, b, b.cardTitle ?: b.author) else Hit(ref, b, snip, if (typed) marks(snip, q) else emptyList())
            }
        }.sortedByDescending { it.bubble?.at ?: "" }
        return (byName + inMessages).take(MAX_HITS)
    }

    /** The text around the first match, on one line, with "…" where it was cut. */
    fun snippet(text: String, foldedQuery: String): String {
        val flat = tidy(text).replace(Regex("\\s+"), " ").trim()
        val at = fold(flat).indexOf(foldedQuery)
        if (at < 0) return flat.take(AROUND * 2)
        val from = (at - AROUND).coerceAtLeast(0)
        val to = (at + foldedQuery.length + AROUND).coerceAtMost(flat.length)
        return (if (from > 0) "…" else "") + flat.substring(from, to).trim() + (if (to < flat.length) "…" else "")
    }

    /** AII-195: the text as the reader sees it: [name](link) shows name, **bold** and `code` lose their marks. */
    fun tidy(text: String): String = text
        .replace(Regex("""\[([^\]]+)]\([^)\s]*\)"""), "$1")
        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        .replace(Regex("""`([^`]+)`"""), "$1")

    /** AII-195: where each typed word sits in [snippet] (case and accent insensitive), merged and in order. */
    fun marks(snippet: String, foldedQuery: String): List<IntRange> {
        val hay = fold(snippet)
        val found = foldedQuery.split(' ').filter { it.isNotEmpty() }.flatMap { w ->
            generateSequence(hay.indexOf(w).takeIf { it >= 0 }) { hay.indexOf(w, it + w.length).takeIf { i -> i >= 0 } }
                .map { it until it + w.length }.toList()
        }.sortedBy { it.first }
        val out = ArrayList<IntRange>()
        for (r in found) {
            val last = out.lastOrNull()
            if (last != null && r.first <= last.last + 1) out[out.size - 1] = last.first..maxOf(last.last, r.last) else out.add(r)
        }
        return out
    }

    /** Lower case without accents, keeping one character per character so positions still line up. */
    fun fold(s: String): String = buildString(s.length) {
        for (ch in s) {
            val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).firstOrNull() ?: ch
            append(base.lowercaseChar())
        }
    }
}
