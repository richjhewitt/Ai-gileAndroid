package com.smizflark.aigile.messenger.data

/** Share sheet direct targets (AII-398): which chats, and the shortcut id <-> chat key. Pure, so it is unit tested. */
object ShareTargets {
    const val CATEGORY = "com.smizflark.aigile.messenger.SHARE_TARGET"
    const val MAX = 4
    private const val PREFIX = "chat:"

    /** The same id ChatHeads uses, so one shortcut serves bubbles and sharing. */
    fun id(chat: String) = PREFIX + chat

    /** The chat key in a share's EXTRA_SHORTCUT_ID, or null when it is not one of ours. */
    fun chatKey(shortcutId: String?): String? = shortcutId?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.ifEmpty { null }

    /** The newest [MAX] chats that have a message (same choice as the launcher's top chats, AII-53). */
    fun top(chats: List<ChatRef>) = chats.filter { it.last != null }.sortedByDescending { it.last }.take(MAX)
}
