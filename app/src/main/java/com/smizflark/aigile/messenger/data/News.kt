package com.smizflark.aigile.messenger.data

/** What's new since you last looked, and whether now is quiet hours. Pure, so it is unit tested. */
object News {
    data class Item(val chat: String, val name: String, val latestFromThem: String?, val preview: String? = null)

    /** Chats whose `last` moved past what you've seen or been told about: worth a closer look. */
    fun candidates(chats: List<ChatRef>, seen: Map<String, String>, told: Map<String, String>): List<ChatRef> =
        chats.filter { c -> c.last != null && c.last > maxOf(seen[c.key] ?: "", told[c.key] ?: "") }

    /**
     * Messages from them newer than seen and told. A chat never looked at before is only recorded
     * (no alert for old history on the first run). Returns the news and the updated "told" map.
     */
    fun diff(items: List<Item>, seen: Map<String, String>, told: Map<String, String>): Pair<List<Item>, Map<String, String>> {
        val next = told.toMutableMap()
        val news = mutableListOf<Item>()
        for (i in items) {
            val at = i.latestFromThem ?: continue
            val known = seen.containsKey(i.chat) || told.containsKey(i.chat)
            if (known && at > maxOf(seen[i.chat] ?: "", told[i.chat] ?: "")) news += i
            if ((next[i.chat] ?: "") < at) next[i.chat] = at
        }
        return news to next
    }

    /**
     * Unread messages for the chats list (AII-25). The Lead Scrum Master: the board's own count, so reading it
     * on the PC clears it here. Others: messages from them after what you've seen, from the saved chat;
     * a friend with a newer message from them counts at least 1. 0 with a moved chat shows as a dot.
     */
    fun unread(c: ChatRef, seen: String?, saved: Conversation?): Int {
        if (c.kind == Kind.WATCHMAN) return c.unread
        val since = seen ?: ""
        val n = saved?.bubbles?.count { !it.mine && it.at > since } ?: 0
        return if (c.kind == Kind.FRIEND && (c.lastIn ?: "") > since) maxOf(n, 1) else n
    }

    /** The first message from them after [seen] (AII-100), where the "New messages" line goes; null when none or never opened. */
    fun firstNew(bubbles: List<Bubble>, seen: String?): String? =
        if (seen == null) null else bubbles.firstOrNull { !it.mine && it.at > seen }?.key

    /** Has anything moved since you last looked? The Lead Scrum Master goes by the board's count. */
    fun fresh(c: ChatRef, seen: String?): Boolean =
        if (c.kind == Kind.WATCHMAN) c.unread > 0 else c.last != null && c.last > (seen ?: "")

    /** Up to where "mark as read" reads a chat: its newest message, saved or listed. */
    fun readUpTo(c: ChatRef, saved: Conversation?): String? =
        listOfNotNull(c.last, c.lastIn, saved?.bubbles?.lastOrNull()?.at).maxOrNull()
    /** Drops muted chats from an alert (AII-48). */
    fun audible(news: List<Item>, muted: Set<String>): List<Item> = news.filter { it.chat !in muted }

    /** Quiet from `from` o'clock to `to` o'clock, across midnight when from > to. */
    fun quiet(hour: Int, from: Int, to: Int): Boolean = when {
        from == to -> false
        from < to -> hour in from until to
        else -> hour >= from || hour < to
    }

    /** One line for the notification: "AI-gile, Tunio" or "AI-gile and 3 more". */
    fun summary(items: List<Item>, more: String = "%1\$s and %2\$d more"): String = when (items.size) {
        0 -> ""
        1 -> items[0].name
        2 -> "${items[0].name}, ${items[1].name}"
        else -> more.format(items[0].name, items.size - 1)
    }
}
