package com.smizflark.aigile.messenger.newmsg

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import java.time.Instant

/** Journey 4: who you can start a new 💬 message with, and when Send is allowed. No Android here, so it unit-tests. */
object NewMessage {

    /** Every project's team (not the 🧭 Lead Scrum Master, not friends), busiest first, then by name. */
    fun teams(chats: List<ChatRef>): List<ChatRef> =
        chats.filter { it.kind == Kind.LEAD }
            .sortedWith(compareByDescending<ChatRef> { at(it.last) }.thenBy { it.name.lowercase() })

    /** Teams whose name or slug has the typed text in it. */
    fun search(teams: List<ChatRef>, query: String): List<ChatRef> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return teams
        return teams.filter { q in it.name.lowercase() || q in it.id.lowercase() }
    }

    /** The team picked to start with: the one you last wrote to if it is still there, else the busiest. */
    fun preselect(teams: List<ChatRef>, lastSlug: String?): ChatRef? =
        teams.firstOrNull { it.id == lastSlug } ?: teams.firstOrNull()

    fun canSend(team: ChatRef?, text: String, sending: Boolean = false) = team != null && text.isNotBlank() && !sending

    private fun at(iso: String?): Instant = runCatching { Instant.parse(iso) }.getOrNull() ?: Instant.EPOCH
}
