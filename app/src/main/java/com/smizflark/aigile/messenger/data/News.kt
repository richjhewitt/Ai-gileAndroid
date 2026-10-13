package com.smizflark.aigile.messenger.data

/** What's new since you last looked, and whether now is quiet hours. Pure, so it is unit tested. */
object News {
    data class Item(val chat: String, val name: String, val latestFromThem: String?)

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

    /** Drops muted chats from an alert (AII-48). */
    fun audible(news: List<Item>, muted: Set<String>): List<Item> = news.filter { it.chat !in muted }

    /** Quiet from `from` o'clock to `to` o'clock, across midnight when from > to. */
    fun quiet(hour: Int, from: Int, to: Int): Boolean = when {
        from == to -> false
        from < to -> hour in from until to
        else -> hour >= from || hour < to
    }

    /** One line for the notification: "AI-gile, Tunio" or "AI-gile and 3 more". */
    fun summary(items: List<Item>): String = when (items.size) {
        0 -> ""
        1 -> items[0].name
        2 -> "${items[0].name}, ${items[1].name}"
        else -> "${items[0].name} and ${items.size - 1} more"
    }
}
