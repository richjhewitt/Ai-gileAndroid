package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatFilter
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTest {
    private val lead = ChatRef(Kind.LEAD, "a", "AI-gile", "", "", waiting = 2)
    private val quiet = ChatRef(Kind.LEAD, "b", "LocoRadio", "", "")
    private val watch = ChatRef(Kind.WATCHMAN, "w", "Lead Scrum Master", "", "")
    private val friend = ChatRef(Kind.FRIEND, "d", "Davy", "", "")
    private val all = listOf(lead, quiet, watch, friend)

    private fun of(f: ChatFilter) = all.filter(f::keep)

    @Test fun allKeepsEverything() = assertEquals(all, of(ChatFilter.ALL))
    @Test fun waitingOnlyWhereAQuestionWaits() = assertEquals(listOf(lead), of(ChatFilter.WAITING))
    @Test fun teamsAreLeadsAndTheScrumMaster() = assertEquals(listOf(lead, quiet, watch), of(ChatFilter.TEAMS))
    @Test fun friendsOnlyFriends() = assertEquals(listOf(friend), of(ChatFilter.FRIENDS))
    @Test fun unreadOnlyWhereABadgeShows() = assertEquals(listOf(quiet, friend), all.filter { ChatFilter.UNREAD.keep(it, it == quiet || it == friend) })
    @Test fun scheduledOnlyWhereASendLaterWaits() = assertEquals(listOf(friend), all.filter { ChatFilter.SCHEDULED.keep(it, scheduled = it == friend) })
    @Test fun scheduledChipOnlyWhenNeeded() {
        assertEquals(false, ChatFilter.SCHEDULED in ChatFilter.shown(false, ChatFilter.ALL))
        assertEquals(true, ChatFilter.SCHEDULED in ChatFilter.shown(true, ChatFilter.ALL))
        assertEquals(true, ChatFilter.SCHEDULED in ChatFilter.shown(false, ChatFilter.SCHEDULED))   // sent meanwhile: stays until you pick another
    }
    @Test fun remindersOnlyWhereAReminderIsSet() = assertEquals(listOf(quiet), all.filter { ChatFilter.REMINDERS.keep(it, reminded = it == quiet) })
    @Test fun remindersChipOnlyWhenNeeded() {
        assertEquals(false, ChatFilter.REMINDERS in ChatFilter.shown(false, ChatFilter.ALL, false))
        assertEquals(true, ChatFilter.REMINDERS in ChatFilter.shown(false, ChatFilter.ALL, true))
        assertEquals(true, ChatFilter.REMINDERS in ChatFilter.shown(false, ChatFilter.REMINDERS, false))
    }
    @Test fun unreadComesRightAfterAll() = assertEquals(ChatFilter.UNREAD, ChatFilter.entries[1])
}
