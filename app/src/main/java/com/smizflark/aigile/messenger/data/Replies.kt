package com.smizflark.aigile.messenger.data

/** Your own quick replies (AII-180), typed in Settings one per line. */
object Replies {
    /** The replies in the order typed: blank lines and repeats left out. Empty means the app's starting ones. */
    fun parse(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
