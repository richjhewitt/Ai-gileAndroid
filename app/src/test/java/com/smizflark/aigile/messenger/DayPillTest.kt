package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.Row
import com.smizflark.aigile.messenger.ui.daysAbove
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-117: which day the floating pill shows for the row at the top of the screen. */
class DayPillTest {
    @Test fun eachRowTakesTheDayAboveIt() {
        // Newest first, as the chat list holds them.
        val rows = listOf(Row.New, Row.Day("d2", "Today"), Row.Card("c", "AIG-1", "Card", false), Row.Day("d1", "Yesterday"))
        assertEquals(listOf("Today", null, "Yesterday", null), daysAbove(rows))
    }

    @Test fun nothingAboveMeansNoPill() {
        assertEquals(listOf<String?>(null), daysAbove(listOf(Row.New)))
        assertEquals(emptyList<String?>(), daysAbove(emptyList()))
    }
}
