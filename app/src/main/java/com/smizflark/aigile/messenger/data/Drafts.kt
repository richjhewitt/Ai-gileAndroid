package com.smizflark.aigile.messenger.data

/** Half-typed replies, one per chat (AII-42). Pure, so it is unit tested; Store keeps them on disk. */
object Drafts {
    /** The drafts with this chat's text set; blank text drops the chat. */
    fun put(all: Map<String, String>, chat: String, text: String): Map<String, String> =
        if (text.isBlank()) all - chat else all + (chat to text)

    /** AII-369: the chats with a non-blank draft, for the Drafts chip. */
    fun chatKeys(all: Map<String, String>): Set<String> = all.filterValues { it.isNotBlank() }.keys
}
