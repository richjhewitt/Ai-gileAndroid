package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Pause
import com.smizflark.aigile.messenger.work.PauseTileLogic
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseTileTest {
    private val now = 1_800_000_000_000L

    @Test fun tapWhenOnPausesForAnHour() {
        assertEquals(now + Pause.HOUR, PauseTileLogic.tapped(now, 0L))
        assertEquals(now + Pause.HOUR, PauseTileLogic.tapped(now, now - 1))
    }

    @Test fun tapWhenPausedTurnsAlertsBackOn() {
        assertEquals(0L, PauseTileLogic.tapped(now, now + 5000))
        assertTrue(!Pause.paused(now, PauseTileLogic.tapped(now, now + 5000)))
    }

    @Test fun endTextIsTheClockTime() {
        val zone = ZoneId.of("UTC")
        val at = java.time.ZonedDateTime.of(2026, 3, 4, 7, 15, 0, 0, zone).toInstant().toEpochMilli()
        val text = PauseTileLogic.endText(at, zone)
        assertTrue(text, text.contains("7") && text.contains("15"))
    }
}
