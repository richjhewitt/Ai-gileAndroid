package com.smizflark.aigile.messenger.data

import com.smizflark.aigile.messenger.R

/** The chips above the chats list (AII-50). */
enum class ChatFilter(val label: String, @androidx.annotation.StringRes val res: Int) {
    ALL("All", R.string.filter_all), WAITING("Waiting on me", R.string.filter_waiting),
    TEAMS("Teams", R.string.filter_teams), FRIENDS("Friends", R.string.filter_friends);

    fun keep(c: ChatRef) = when (this) {
        ALL -> true
        WAITING -> c.waiting > 0
        TEAMS -> c.kind != Kind.FRIEND   // leads and the Lead Scrum Master
        FRIENDS -> c.kind == Kind.FRIEND
    }
}
