package com.smizflark.aigile.messenger.data

/** Where you left a chat (AII-72): the first visible row by id (not index, so new messages don't shift it), its offset, and the newest message you had seen. */
data class ScrollPlace(val key: String, val offset: Int, val seen: String?)

/** Remembers the place per chat for as long as the app lives. */
object ScrollPlaces {
    private val places = HashMap<String, ScrollPlace>()

    @Synchronized fun save(chat: String, place: ScrollPlace) { places[chat] = place }
    @Synchronized fun get(chat: String): ScrollPlace? = places[chat]
    /** At the newest message there is nothing to keep: the chat opens at the bottom. */
    @Synchronized fun clear(chat: String) { places.remove(chat) }
    @Synchronized fun clearAll() = places.clear()

    /** Index in the newest-first row ids to scroll back to, or null when that row is gone. */
    fun indexOf(place: ScrollPlace, rowIdsNewestFirst: List<String>): Int? =
        rowIdsNewestFirst.indexOf(place.key).takeIf { it >= 0 }
}
