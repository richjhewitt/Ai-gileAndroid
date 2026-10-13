package com.smizflark.aigile.messenger.chats

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** What a row in the chats list shows beyond the ChatRef. Pure, so it is unit tested. */
object ChatList {

    /** Unread dot: the chat moved past the newest message you've looked at. */
    fun unread(c: ChatRef, seen: Map<String, String>): Boolean =
        c.last != null && c.last > (seen[c.key] ?: "")

    /** "14:05" today, "Yesterday", "Mon" this week, "12 Oct" this year, "12 Oct 2025" before. */
    fun timeLabel(iso: String?, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        val at = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return ""
        val day = at.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(day, today)
        val f = { p: String -> DateTimeFormatter.ofPattern(p, Locale.UK).format(at.atZone(zone)) }
        return when {
            days <= 0L -> f("HH:mm")
            days == 1L -> "Yesterday"
            days < 7L -> f("EEE")
            day.year == today.year -> f("d MMM")
            else -> f("d MMM yyyy")
        }
    }

    /** Section headings in list order: leads (and the Lead Scrum Master) first, then friends. */
    fun sections(chats: List<ChatRef>): List<Pair<String, List<ChatRef>>> {
        val (friends, teams) = chats.partition { it.kind == Kind.FRIEND }
        return listOf("Teams" to teams, "Friends" to friends).filter { it.second.isNotEmpty() }
    }
}
