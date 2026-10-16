package com.smizflark.aigile.messenger.data

/** Select several messages (AII-371): which are picked, and their text for Copy and Share. Pure, so it is unit tested. */
object Selection {
    /** Picks the message, or takes it off if it was picked. */
    fun toggle(picked: Set<String>, key: String): Set<String> = if (key in picked) picked - key else picked + key

    /**
     * The picked messages as plain text, oldest first whatever order they were picked in, in Transcript's style:
     * "who, time:" then the text, a blank line between. [bubbles] is the chat's messages; [plain] gives the text as read (markdown rendered).
     */
    fun text(bubbles: List<Bubble>, picked: Set<String>, chat: String, owner: String, time: (String) -> String, plain: (Bubble) -> String = { it.text }): String =
        bubbles.filter { it.key in picked }.sortedBy { it.at }.joinToString("\n\n") { b ->
            val who = if (b.mine) owner else b.author.ifBlank { chat }
            val pics = b.pics.map { "[${it.name.ifBlank { it.path.substringAfterLast('/') }}]" }
            val body = (pics + plain(b).trim()).filter { it.isNotBlank() }.joinToString("\n")
            "$who, ${time(b.at)}:\n$body"
        }
}
