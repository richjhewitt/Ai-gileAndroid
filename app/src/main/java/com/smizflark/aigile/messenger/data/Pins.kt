package com.smizflark.aigile.messenger.data

/** Pinned chats first (AII-47), each group keeping the board's order. */
fun pinFirst(chats: List<ChatRef>, pinned: Set<String>): List<ChatRef> {
    if (pinned.isEmpty()) return chats
    val (top, rest) = chats.partition { it.key in pinned }
    return top + rest
}

/** Pins or unpins one chat. */
fun togglePin(pinned: Set<String>, key: String) = if (key in pinned) pinned - key else pinned + key

/** Muted chats sink below the others (AII-221), keeping their order; pinned chats stay first, muted or not. */
fun mutedLast(chats: List<ChatRef>, pinned: Set<String>, muted: Set<String>): List<ChatRef> {
    if (muted.isEmpty()) return pinFirst(chats, pinned)
    val (top, rest) = chats.partition { it.key in pinned }
    val (quiet, loud) = rest.partition { it.key in muted }
    return top + loud + quiet
}

/** Sort chats (AII-241, AII-404): the board's order (newest first), A to Z by name, or "unread" (unread chats first, each group keeping newest-first). Applied before pins and muted-last, so those still win. */
fun sortChats(chats: List<ChatRef>, sort: String, unread: (ChatRef) -> Boolean = { false }): List<ChatRef> = when (sort) {
    "az" -> chats.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    "unread" -> chats.partition(unread).let { (u, rest) -> u + rest }
    else -> chats
}
