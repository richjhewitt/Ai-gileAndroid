package com.smizflark.aigile.messenger.data

/** Pinned chats first (AII-47), each group keeping the board's order. */
fun pinFirst(chats: List<ChatRef>, pinned: Set<String>): List<ChatRef> {
    if (pinned.isEmpty()) return chats
    val (top, rest) = chats.partition { it.key in pinned }
    return top + rest
}

/** Pins or unpins one chat. */
fun togglePin(pinned: Set<String>, key: String) = if (key in pinned) pinned - key else pinned + key
