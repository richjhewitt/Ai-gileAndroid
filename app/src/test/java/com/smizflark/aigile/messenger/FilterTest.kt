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
}
