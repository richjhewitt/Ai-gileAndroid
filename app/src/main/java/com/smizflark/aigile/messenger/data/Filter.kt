package com.smizflark.aigile.messenger.data

/** The chips above the chats list (AII-50). */
enum class ChatFilter(val label: String) {
    ALL("All"), WAITING("Waiting on me"), TEAMS("Teams"), FRIENDS("Friends");

    fun keep(c: ChatRef) = when (this) {
        ALL -> true
        WAITING -> c.waiting > 0
        TEAMS -> c.kind != Kind.FRIEND   // leads and the Lead Scrum Master
        FRIENDS -> c.kind == Kind.FRIEND
    }
}
