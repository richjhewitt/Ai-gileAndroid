package com.smizflark.aigile.messenger.data

/** Share a whole chat as text (AII-139): who, when, what, oldest first. Pure, so it is unit tested. */
object Transcript {
    fun of(c: Conversation, owner: String, time: (String) -> String): String {
        val head = c.ref.name
        val body = c.bubbles.joinToString("\n\n") { b ->
            val who = if (b.mine) owner else b.author.ifBlank { c.ref.name }
            val pics = b.pics.map { "[${it.name.ifBlank { it.path.substringAfterLast('/') }}]" }
            val text = (pics + b.text.trim()).filter { it.isNotBlank() }.joinToString("\n")
            "$who, ${time(b.at)}:\n$text"
        }
        return "$head\n\n$body\n"
    }
}
