package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Search
import com.smizflark.aigile.messenger.data.Star
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Search across all chats (AII-30). */
class SearchTest {
    private val gile = ChatRef(Kind.LEAD, "ai-gile", "AI-gile", "Lead", "🗂️")
    private val radio = ChatRef(Kind.LEAD, "loco-radio", "LocoRadio", "Lead", "📻")
    private val ann = ChatRef(Kind.FRIEND, "a", "Ann", "", "🙂")

    private fun b(text: String, at: String, card: String? = null, title: String? = null, author: String = "Lead") =
        Bubble("$card:$at", author, text, at, mine = author == "Rich", card = card, cardTitle = title)

    private val convs = listOf(
        Conversation(gile, listOf(
            b("The radio player is fixed", "2026-10-01T10:00:00Z", "AIG-1", "💬 Player"),
            b("Café menu is next", "2026-10-03T10:00:00Z", "AIG-2", "💬 Menu"),
        )),
        Conversation(radio, listOf(b("New station list uploaded", "2026-10-02T10:00:00Z", "LOC-9", "💬 Stations"))),
        Conversation(ann, listOf(b("See you at the cafe", "2026-10-04T10:00:00Z", author = "Ann"))),
    )

    @Test fun chatNamesComeFirstThenMessagesNewestFirst() {
        val hits = Search.run("radio", listOf(gile, radio, ann), convs)
        assertEquals("loco-radio", hits[0].ref.id); assertNull(hits[0].bubble)
        assertEquals(listOf("AIG-1"), hits.drop(1).map { it.bubble?.card })
        assertEquals(listOf("AIG-2", "LOC-9", "AIG-1"), Search.run("is", listOf(), convs).map { it.bubble?.card })
    }

    @Test fun starredKeepsOnlyStarredMessagesThatMatchNewestFirst() {   // AII-246
        val all = listOf(gile, radio, ann)
        val stars = listOf(
            Star("lead:ai-gile", "AIG-1:2026-10-01T10:00:00Z", "Lead", "x", "2026-10-01T10:00:00Z"),
            Star("lead:loco-radio", "LOC-9:2026-10-02T10:00:00Z", "Lead", "x", "2026-10-02T10:00:00Z"),
            Star("lead:ai-gile", "gone:1", "Lead", "x", "2026-10-05T10:00:00Z"),
        )
        assertEquals(listOf("LOC-9", "AIG-1"), Search.run("", all, convs, starred = stars).map { it.bubble?.card })
        assertEquals(listOf("AIG-1"), Search.run("player", all, convs, starred = stars).map { it.bubble?.card })
        assertTrue(Search.run("cafe", all, convs, starred = stars).isEmpty())
        assertTrue(Search.run("", all, convs, starred = emptyList()).isEmpty())
    }

    @Test fun ignoresCaseAndAccents() {
        val hits = Search.run("CAFE", listOf(gile, radio, ann), convs)
        assertEquals(listOf("friend:a", "lead:ai-gile"), hits.map { it.ref.key })
        assertTrue(Search.run("café", listOf(gile, radio, ann), convs).size == 2)
    }

    @Test fun matchesCardTitlesAndAuthors() {
        assertEquals("LOC-9", Search.run("stations", listOf(radio), convs).single().bubble?.card)
        assertEquals("friend:a", Search.run("ann", listOf(gile, radio), convs).last().ref.key)
    }

    @Test fun tooShortOrBlankFindsNothing() {
        assertTrue(Search.run(" ", listOf(gile), convs).isEmpty())
        assertTrue(Search.run("a", listOf(gile), convs).isEmpty())
    }

    @Test fun snippetCutsAroundTheMatchOnOneLine() {
        val long = "start " + "x".repeat(100) + " needle\nhere " + "y".repeat(100)
        val s = Search.snippet(long, "needle")
        assertTrue(s, s.startsWith("…") && s.endsWith("…") && "needle here" in s && '\n' !in s)
        assertEquals("short needle", Search.snippet("short needle", "needle"))
    }

    // AII-195
    @Test fun snippetShowsMarkdownAsReadAndKeepsTheHit() {
        assertEquals("Read the guide first", Search.snippet("Read [the guide](https://x.com/a_b) first", "guide"))
        assertEquals("a bold and code", Search.snippet("a **bold** and `code`", "bold"))
        val long = "[" + "w ".repeat(60) + "](https://x.com/" + "z".repeat(200) + ") needle"
        assertTrue("needle" in Search.snippet(long, "needle"))
    }

    @Test fun foundWordsAreMarkedIgnoringCaseAndAccents() {
        val s = "Café au LAIT, cafe again"
        assertEquals(listOf(0..3, 14..17), Search.marks(s, Search.fold("cafe")))
        assertEquals(listOf(0..3, 8..11, 14..17), Search.marks(s, Search.fold("café lait")))
        assertTrue(Search.marks(s, "zzz").isEmpty())
    }

    @Test fun hitCarriesMarksOnItsSnippet() {
        val c = Conversation(gile, listOf(b("See [Docs](https://x.com) for **Docs** now", "2026-10-01T10:00:00Z", "AIG-1")))
        val h = Search.run("docs", listOf(gile), listOf(c)).single()
        assertEquals("See Docs for Docs now", h.snippet)
        assertEquals(listOf(4..7, 13..16), h.marks)
    }

    @Test fun capsTheNumberOfHits() {
        val many = Conversation(gile, (1..300).map { b("ping $it", "2026-10-01T10:%02d:00Z".format(it % 60), "AIG-$it") })
        assertEquals(Search.MAX_HITS, Search.run("ping", listOf(gile), listOf(many)).size)
    }

    // AII-192: Pictures and Links chips
    private val pic = com.smizflark.aigile.messenger.data.Pic("/api/a.png", "a.png", "image/png")
    private val chipConvs = listOf(
        Conversation(gile, listOf(
            b("Look at this", "2026-10-01T10:00:00Z", "AIG-1").copy(pics = listOf(pic)),
            b("Read the guide at https://x.io/g now", "2026-10-02T10:00:00Z", "AIG-2"),
            b("Open dnbloco.com please", "2026-10-03T10:00:00Z", "AIG-3"),
            b("No link here, just 3.5 and e.g. file.txt", "2026-10-04T10:00:00Z", "AIG-4"),
            b("doc", "2026-10-05T10:00:00Z", "AIG-5").copy(pics = listOf(com.smizflark.aigile.messenger.data.Pic("/api/a.pdf", "a.pdf", "application/pdf"))),
        )),
    )

    @Test fun linksChipListsEveryLinkNewestFirstWithNoQuery() {
        val hits = Search.run("", listOf(gile), chipConvs, links = true)
        assertEquals(listOf("AIG-3", "AIG-2"), hits.map { it.bubble?.card })
    }

    @Test fun picturesChipOnlyTakesImages() {
        assertEquals(listOf("AIG-1"), Search.run("", listOf(gile), chipConvs, pictures = true).map { it.bubble?.card })
    }

    @Test fun chipsHideChatsAndStillUseTheQuery() {
        assertTrue(Search.run("ai-gile", listOf(gile), chipConvs, links = true).isEmpty())
        assertEquals(listOf("AIG-3"), Search.run("dnbloco", listOf(gile), chipConvs, links = true).map { it.bubble?.card })
        assertTrue(Search.run("", listOf(gile), chipConvs).isEmpty())
    }

    // AII-258: Last 7 days chip
    @Test fun last7ChipKeepsOnlyTheLastSevenDaysAndCombinesWithTheQuery() {
        val z = java.time.ZoneId.systemDefault()
        val old = com.smizflark.aigile.messenger.data.Now.clock
        com.smizflark.aigile.messenger.data.Now.clock = java.time.Clock.fixed(java.time.LocalDate.of(2026, 10, 14).atTime(12, 0).atZone(z).toInstant(), z)
        try {
            fun at(d: Int) = java.time.LocalDate.of(2026, 10, d).atTime(12, 0).atZone(z).toInstant().toString()
            val convs = listOf(Conversation(gile, listOf(b("old news", at(7), "AIG-1"), b("edge news", at(8), "AIG-2"), b("fresh news", at(14), "AIG-3"))))
            assertEquals(listOf("AIG-3", "AIG-2"), Search.run("", listOf(gile), convs, last7 = true).map { it.bubble?.card })
            assertEquals(listOf("AIG-3"), Search.run("fresh", listOf(gile), convs, last7 = true).map { it.bubble?.card })
        } finally { com.smizflark.aigile.messenger.data.Now.clock = old }
    }
}
