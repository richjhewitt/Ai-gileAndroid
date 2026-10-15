package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipsTest {
    @Test fun firstDemoOpenShows() = assertTrue(Tips.show(demo = true, seen = false))
    @Test fun afterGotItHidden() = assertFalse(Tips.show(demo = true, seen = true))
    @Test fun realBoardNeverShows() = assertFalse(Tips.show(demo = false, seen = false))
}
