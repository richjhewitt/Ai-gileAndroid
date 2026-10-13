package com.smizflark.aigile.messenger.newmsg

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewMessageTest {

    private fun lead(id: String, name: String, last: String? = null) = ChatRef(Kind.LEAD, id, name, "", "📁", last = last)

    private val radio = lead("locoradio", "LocoRadio", "2026-10-13T09:00:00Z")
    private val board = lead("ai-gile", "AI-gile", "2026-10-13T10:00:00Z")
    private val mail = lead("smizflarkdomainstuff", "SmizflarkDomainStuff")
    private val quiet = lead("zed", "Zed")
    private val chats = listOf(
        radio, mail, quiet, board,
        ChatRef(Kind.WATCHMAN, "_watchman", "Lead Scrum Master", "", "🧭", last = "2026-10-13T11:00:00Z"),
        ChatRef(Kind.FRIEND, "f1", "Sam", "", "🔒", last = "2026-10-13T12:00:00Z"),
    )

    @Test fun only_teams_busiest_first_then_by_name() {
        assertEquals(listOf("ai-gile", "locoradio", "smizflarkdomainstuff", "zed"), NewMessage.teams(chats).map { it.id })
    }

    @Test fun search_matches_name_or_slug_any_case() {
        val teams = NewMessage.teams(chats)
        assertEquals(listOf("locoradio"), NewMessage.search(teams, " RADIO ").map { it.id })
        assertEquals(listOf("smizflarkdomainstuff"), NewMessage.search(teams, "domainstuff").map { it.id })
        assertEquals(teams, NewMessage.search(teams, "  "))
        assertTrue(NewMessage.search(teams, "nothing like it").isEmpty())
    }

    @Test fun preselects_the_last_team_written_to_else_the_busiest() {
        val teams = NewMessage.teams(chats)
        assertEquals("zed", NewMessage.preselect(teams, "zed")?.id)
        assertEquals("ai-gile", NewMessage.preselect(teams, "gone-project")?.id)
        assertEquals("ai-gile", NewMessage.preselect(teams, null)?.id)
        assertNull(NewMessage.preselect(emptyList(), "zed"))
    }

    @Test fun send_needs_a_team_and_some_text() {
        assertTrue(NewMessage.canSend(radio, "Hello"))
        assertFalse(NewMessage.canSend(null, "Hello"))
        assertFalse(NewMessage.canSend(radio, "  \n "))
        assertFalse(NewMessage.canSend(radio, "Hello", sending = true))
    }
}
