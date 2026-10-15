package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    @Test fun newInstallShowsNothing() = assertFalse(WhatsNew.show(0, 300))
    @Test fun sameVersionShowsNothing() = assertFalse(WhatsNew.show(300, 300))
    @Test fun newerVersionShows() = assertTrue(WhatsNew.show(299, 300))
}
