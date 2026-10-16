package com.smizflark.aigile.messenger.data

import java.text.Normalizer

/** Settings search (AII-403): case- and accent-insensitive; every typed word must be found in one row's labels. */
object SettingsFind {
    fun normalise(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()

    /** True for an empty query, or when every query word is part of some label of the row. */
    fun matches(query: String, labels: List<String>): Boolean {
        val words = normalise(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val hay = normalise(labels.joinToString(" ")).split(' ')
        return words.all { w -> hay.any { it.startsWith(w) } }   // start of a word, so "lock" does not find "block"
    }

    fun active(query: String) = normalise(query).isNotEmpty()
}
