package com.smizflark.aigile.messenger.data

/** Recent searches (AII-155): the last few, newest first, kept on this phone. Pure, so it is unit tested. */
object Recent {
    const val MAX = 5

    /** Puts [query] first; a repeat (any case) moves up instead of showing twice; too short to search is left out. */
    fun add(all: List<String>, query: String): List<String> {
        val q = query.trim()
        if (q.length < 2) return all
        return (listOf(q) + all.filterNot { it.equals(q, ignoreCase = true) }).take(MAX)
    }
}
