package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Mute
import com.smizflark.aigile.messenger.data.Pause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-157: mute a chat for a while; after the time its alerts come back by themselves. */
class MuteForTest {
    private val now = 1_000_000L

    @Test fun oneHourAndEightHours() {
        assertEquals(now + Pause.HOUR, Mute.until(now, Mute.For.HOUR))
        assertEquals(now + 8 * Pause.HOUR, Mute.until(now, Mute.For.EIGHT_HOURS))
        assertNull(Mute.until(now, Mute.For.ALWAYS))
    }

    @Test fun aTimedMuteEndsByItself() {
        val ends = mapOf("lead:a" to now + Pause.HOUR)
        assertEquals(setOf("lead:a", "lead:b"), Mute.active(setOf("lead:a", "lead:b"), ends, now))
        assertEquals(setOf("lead:b"), Mute.active(setOf("lead:a", "lead:b"), ends, now + Pause.HOUR))
    }
}
