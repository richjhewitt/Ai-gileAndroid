package com.smizflark.aigile.messenger.data

/**
 * The chat the "Next unread" button at the bottom of a chat goes to (AII-204): the first chat with unread
 * messages in chats-list order (pins first), skipping [current], muted chats and closed (hidden) ones. Null when none.
 */
fun nextUnread(chats: List<ChatRef>, pinned: Set<String>, unread: Map<String, Int>, muted: Set<String>, hidden: Set<String>, current: String): ChatRef? =
    pinFirst(chats, pinned).firstOrNull { it.key != current && it.key !in muted && it.key !in hidden && (unread[it.key] ?: 0) > 0 }
