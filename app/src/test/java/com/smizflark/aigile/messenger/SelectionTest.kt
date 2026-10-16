package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Selection
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-371: picking several messages and their joined text. */
class SelectionTest {
    private val all = listOf(
        Bubble("1", "Lead", "Ready?", "t1", false),
        Bubble("2", "", "Yes", "t2", true, pics = listOf(Pic("http://b/x/sea.png", "sea.png"))),
        Bubble("3", "", "Done", "t3", false),
    )

    @Test fun toggleAddsThenRemoves() {
        val one = Selection.toggle(emptySet(), "2")
        assertEquals(setOf("2"), one)
        assertEquals(setOf("2", "3"), Selection.toggle(one, "3"))
        assertEquals(emptySet<String>(), Selection.toggle(one, "2"))
    }

    @Test fun textIsOldestFirstWhateverTheOrderPicked() {
        assertEquals("Rich, T2:\n[sea.png]\nYes\n\nAI-gile, T3:\nDone",
            Selection.text(all, linkedSetOf("3", "2"), "AI-gile", "Rich", { it.uppercase() }))
    }

    @Test fun nothingPickedIsEmpty() {
        assertEquals("", Selection.text(all, emptySet(), "AI-gile", "Rich", { it }))
    }

    @Test fun usesTheReadableTextGiven() {
        assertEquals("Lead, t1:\nREADY?", Selection.text(all, setOf("1"), "AI-gile", "Rich", { it }, plain = { it.text.uppercase() }))
    }
}
