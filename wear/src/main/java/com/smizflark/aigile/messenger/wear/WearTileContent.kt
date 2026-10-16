package com.smizflark.aigile.messenger.wear

/** What the tile shows (AII-415): the unread count and the top 3 chat names, unread first, then newest. */
data class TileContent(val locked: Boolean, val unread: Int, val top: List<WearChat>) {
    companion object {
        const val MAX = 3

        fun build(chats: List<WearChat>, locked: Boolean): TileContent {
            if (locked) return TileContent(true, 0, emptyList())
            val top = chats.sortedWith(compareByDescending<WearChat> { it.unread }.thenByDescending { it.time }).take(MAX)
            return TileContent(false, chats.count { it.unread }, top)
        }
    }
}
