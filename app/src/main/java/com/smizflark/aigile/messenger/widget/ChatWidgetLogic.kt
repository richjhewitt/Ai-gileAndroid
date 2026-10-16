package com.smizflark.aigile.messenger.widget

import com.smizflark.aigile.messenger.data.Bubble

/* ---------- One chat widget (AII-380): pure logic, no Android, so it can be tested ---------- */

object ChatWidgetLogic {
    /** The newest [max] messages of a chat, oldest first, as one line each: "who: short text". */
    fun lines(bubbles: List<Bubble>, you: String, max: Int = 3, maxChars: Int = 60, clearedAt: String? = null): List<String> =
        com.smizflark.aigile.messenger.data.Cleared.usable(bubbles, clearedAt).filter { it.text.isNotBlank() }   // AII-405
            .sortedBy { it.at }.takeLast(max)
            .map { "${if (it.mine) you else it.author.ifBlank { "?" }}: ${trim(it.text, maxChars)}" }

    /** One line, whitespace squeezed, cut with an ellipsis when longer than [max]. */
    fun trim(text: String, max: Int): String {
        val s = text.replace(Regex("\\s+"), " ").trim()
        return if (s.length <= max) s else s.take(max - 1).trimEnd() + "…"
    }

    /* Which chat each widget shows: stored as "widgetId=chatKey" entries. */
    fun encode(map: Map<Int, String>): Set<String> = map.map { "${it.key}=${it.value}" }.toSet()

    fun decode(entries: Set<String>): Map<Int, String> = entries.mapNotNull { e ->
        val i = e.indexOf('=')
        val id = if (i > 0) e.substring(0, i).toIntOrNull() else null
        if (id == null || i == e.length - 1) null else id to e.substring(i + 1)
    }.toMap()

    fun with(map: Map<Int, String>, id: Int, chat: String): Map<Int, String> = map + (id to chat)

    /** Forget widgets that are gone from the home screen. */
    fun without(map: Map<Int, String>, ids: Collection<Int>): Map<Int, String> = map.filterKeys { it !in ids }
}
