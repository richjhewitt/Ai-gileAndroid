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

/** Sort chats (AII-241): the board's order (newest first) or A to Z by name. Applied before pins and muted-last, so those still win. */
fun sortChats(chats: List<ChatRef>, azSort: Boolean): List<ChatRef> =
    if (azSort) chats.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }) else chats
