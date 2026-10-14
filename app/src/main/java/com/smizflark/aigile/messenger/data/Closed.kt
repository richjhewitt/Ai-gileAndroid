package com.smizflark.aigile.messenger.data

/** Close a chat (AII-131): it leaves the list until a newer message comes, like the board's Messages list (AIG-917). Pure, so it is unit tested. */
object Closed {
    /** The chat's key in the board's closed list (/api/dmclosed). */
    fun boardKey(c: ChatRef) = when (c.kind) {
        Kind.LEAD -> c.id
        Kind.WATCHMAN -> "_watchman"
        Kind.FRIEND -> "dm:${c.id}"
    }

    /** Off the list while nothing newer came in since it was closed and nothing waits on you. */
    fun hidden(c: ChatRef, closedAt: String?) = closedAt != null && c.waiting == 0 && (c.last == null || c.last <= closedAt)

    /** The board's list, keyed by this app's chat keys. */
    fun fromBoard(board: Map<String, String>, chats: List<ChatRef>): Map<String, String> =
        chats.mapNotNull { c -> board[boardKey(c)]?.let { c.key to it } }.toMap()
}
