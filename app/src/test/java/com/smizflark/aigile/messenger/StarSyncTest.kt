package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Star
import com.smizflark.aigile.messenger.data.Stars
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-141: stars kept in step with the board. */
class StarSyncTest {
    private val t1 = "2026-10-14T09:00:00Z"
    private val t2 = "2026-10-14T10:00:00Z"
    private fun b(card: String, at: String, i: Int) = Bubble("$card:$at:$i", "Lead", "hi $i", at, false)
    private val bubbles = listOf(b("_lead", t1, 0), b("AII-5", t2, 1))

    @Test fun boardKeyIsFoldAndTime() = assertEquals("AII-5|$t2", Stars.boardKey(Stars.of("lead:a", bubbles[1])))

    @Test fun theBoardsListWinsForThisChatOnly() {
        val other = Star("lead:b", "_lead:$t1:0", "X", "other chat", t1)
        val old = Stars.of("lead:a", bubbles[0])
        val got = Stars.fromBoard(listOf(other, old), "lead:a", listOf("AII-5|$t2"), bubbles)
        assertEquals(listOf(other, Stars.of("lead:a", bubbles[1])), got)
    }

    @Test fun aBoardStarNotLoadedHereIsLeftOut() =
        assertEquals(emptyList<Star>(), Stars.fromBoard(emptyList(), "lead:a", listOf("AII-9|$t1"), bubbles))

    @Test fun firstSyncKeepsBothSidesStars() {
        val mine = Stars.of("lead:a", bubbles[0])
        assertEquals(listOf("_lead|$t1", "AII-5|$t2"), Stars.firstSync(listOf(mine), "lead:a", listOf("AII-5|$t2", "_lead|$t1")))
    }

    @Test fun outOfStepWhenTheBoardHasADifferentStar() {
        val mine = listOf(Stars.of("lead:a", bubbles[0]))
        assertEquals(true, Stars.outOfStep(mine, "lead:a", listOf("AII-5|$t2")))
        assertEquals(false, Stars.outOfStep(mine, "lead:a", listOf("_lead|$t1")))
        assertEquals(false, Stars.outOfStep(mine, "lead:a", null))
        assertEquals(true, Stars.outOfStep(emptyList(), "lead:a", listOf("_lead|$t1")))
    }

    @Test fun aStarAlreadyHereKeepsItsText() {
        val kept = Star("lead:a", "_lead:$t1:7", "Lead", "saved text", t1)
        assertEquals(listOf(kept), Stars.fromBoard(listOf(kept), "lead:a", listOf("_lead|$t1"), emptyList()))
    }

    @Test fun searchKeepsMatchingWordsOrSender() {
        val a = Star("lead:a", "k1", "Lead", "The APK is ready", t1)
        val b = Star("lead:a", "k2", "Rich", "lunch?", t2)
        val all = listOf("x" to a, "x" to b)
        assertEquals(listOf("x" to a), Stars.matching(all, " apk "))
        assertEquals(listOf("x" to b), Stars.matching(all, "rich"))
        assertEquals(all, Stars.matching(all, "  "))
    }
}
