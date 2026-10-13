package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.News
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-48: a muted chat gives no alert, and unmuting doesn't replay what came in meanwhile. */
class MuteTest {
    private val a = News.Item("lead:a", "AI-gile", "2026-10-13T10:00:00Z")
    private val b = News.Item("lead:b", "Tunio", "2026-10-13T10:05:00Z")

    @Test fun mutedChatIsDropped() = assertEquals(listOf(b), News.audible(listOf(a, b), setOf("lead:a")))

    @Test fun nothingMutedKeepsAll() = assertEquals(listOf(a, b), News.audible(listOf(a, b), emptySet()))

    @Test fun mutedChatStillMovesOnSoUnmutingIsQuiet() {
        val seen = mapOf("lead:a" to "2026-10-13T09:00:00Z")
        val (news, told) = News.diff(listOf(a), seen, emptyMap())
        assertEquals(emptyList<News.Item>(), News.audible(news, setOf("lead:a")))
        // Unmuted later, the same message is already "told": no alert for it.
        assertEquals(emptyList<News.Item>(), News.diff(listOf(a), seen, told).first)
    }
}
