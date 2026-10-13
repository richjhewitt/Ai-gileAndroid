package com.smizflark.aigile.messenger.data

/** What the chat details view lists (AII-80): the cards still open and the pictures sent in the chat. */
data class ChatInfo(val openCards: List<Target>, val pictures: List<Pic>)

/** Open cards are the ones waiting on you; pictures are newest first, each only once. */
fun chatInfo(c: Conversation?): ChatInfo {
    if (c == null) return ChatInfo(emptyList(), emptyList())
    val open = c.targets.filter { it.id != null && it.waiting }
    val pics = c.bubbles.asReversed().flatMap { it.pics }.filter { it.image }.distinctBy { it.path }
    return ChatInfo(open, pics)
}
