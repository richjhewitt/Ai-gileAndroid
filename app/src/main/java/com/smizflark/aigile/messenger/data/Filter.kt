package com.smizflark.aigile.messenger.data

import com.smizflark.aigile.messenger.R

/** The chips above the chats list (AII-50). */
enum class ChatFilter(val label: String, @androidx.annotation.StringRes val res: Int) {
    ALL("All", R.string.filter_all), UNREAD("Unread", R.string.filter_unread), WAITING("Waiting on me", R.string.filter_waiting),
    TEAMS("Teams", R.string.filter_teams), FRIENDS("Friends", R.string.filter_friends), SCHEDULED("Scheduled", R.string.filter_scheduled);

    /** [unread]: the row shows a badge (AII-125). [scheduled]: a Send later message waits in it (AII-152). */
    fun keep(c: ChatRef, unread: Boolean = false, scheduled: Boolean = false) = when (this) {
        ALL -> true
        UNREAD -> unread
        WAITING -> c.waiting > 0
        TEAMS -> c.kind != Kind.FRIEND   // leads and the Lead Scrum Master
        FRIENDS -> c.kind == Kind.FRIEND
        SCHEDULED -> scheduled
    }

    companion object {
        /** The chips to show: Scheduled only while a Send later message waits, or while it is the one picked (AII-152). */
        fun shown(anyScheduled: Boolean, picked: ChatFilter) = entries.filter { it != SCHEDULED || anyScheduled || picked == SCHEDULED }
    }
}

/** AII-250: how many chats show an unread badge (same rule as the Unread chip, AII-125); chats, not messages, and muted chats are left out. */
fun unreadChatCount(chats: List<ChatRef>, badge: (ChatRef) -> Boolean, muted: (ChatRef) -> Boolean): Int = chats.count { badge(it) && !muted(it) }
