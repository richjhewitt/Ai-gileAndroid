package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.WHATS_NEW
import com.smizflark.aigile.messenger.ui.whatsNewShown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    @Test fun keepsOnlyTheThreeNewestBuildsNewestFirst() {
        val lines = listOf(1 to 300, 2 to 410, 3 to 350, 4 to 300, 5 to 200, 6 to 350)
        assertEquals(listOf(2 to 410, 3 to 350, 6 to 350, 1 to 300, 4 to 300), whatsNewShown(lines))
    }

    @Test fun fewerBuildsThanKeepShowsThemAll() {
        assertEquals(listOf(2 to 20, 1 to 10), whatsNewShown(listOf(1 to 10, 2 to 20)))
    }

    @Test fun theRealListShowsSomethingAndNothingOld() {
        val shown = whatsNewShown()
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.map { it.second }.distinct().size <= 3)
        assertEquals(WHATS_NEW.maxOf { it.second }, shown.first().second)
    }
}
