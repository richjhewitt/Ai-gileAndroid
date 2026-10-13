package com.smizflark.aigile.messenger.ui

/** AII-78: find in this chat. Pure, so it can be tested without a screen. */
object Find {
    const val MIN_LETTERS = 2

    /** Indexes of the [texts] that contain [query], ignoring case. Under [MIN_LETTERS] letters (spaces at the ends do not count) nothing matches. */
    fun matches(texts: List<String>, query: String): List<Int> {
        val q = query.trim()
        if (q.length < MIN_LETTERS) return emptyList()
        return texts.indices.filter { texts[it].contains(q, ignoreCase = true) }
    }
}
