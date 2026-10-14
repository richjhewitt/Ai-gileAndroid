package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Pause
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private fun at(s: String) = ZonedDateTime.parse(s).toInstant().toEpochMilli()

    @Test fun pausedOnlyWhileUntilIsAhead() {
        assertFalse(Pause.paused(1000, 0))
        assertTrue(Pause.paused(1000, 2000))
        assertFalse(Pause.paused(2000, 2000))
        assertFalse(Pause.paused(3000, 2000))
    }

    @Test fun hoursAdd() {
        assertEquals(1_000L + 3_600_000L, Pause.oneHour(1_000))
        assertEquals(1_000L + 4 * 3_600_000L, Pause.fourHours(1_000))
    }

    @Test fun tomorrowIsEightLocal() {
        val want = at("2026-03-11T08:00:00+01:00[Europe/Amsterdam]")
        assertEquals(want, Pause.untilTomorrow(at("2026-03-10T15:30:00+01:00[Europe/Amsterdam]"), zone))
        assertEquals(want, Pause.untilTomorrow(at("2026-03-11T00:05:00+01:00[Europe/Amsterdam]"), zone))   // small hours: this morning
        assertEquals(want, Pause.untilTomorrow(at("2026-03-10T08:00:00+01:00[Europe/Amsterdam]"), zone))
        // AII-222: your own morning time
        assertEquals(at("2026-03-11T07:00:00+01:00[Europe/Amsterdam]"), Pause.untilTomorrow(at("2026-03-10T15:30:00+01:00[Europe/Amsterdam]"), zone, hour = 7))
    }

    /** AII-147: a picked time later today stays today; one already gone means tomorrow. */
    @Test fun pickedTimeIsTheNextOne() {
        val now = at("2026-03-10T15:30:00+01:00[Europe/Amsterdam]")
        assertEquals(at("2026-03-10T21:15:00+01:00[Europe/Amsterdam]"), Pause.nextAt(now, 21, 15, zone))
        assertEquals(at("2026-03-11T09:00:00+01:00[Europe/Amsterdam]"), Pause.nextAt(now, 9, 0, zone))
        assertEquals(at("2026-03-11T15:30:00+01:00[Europe/Amsterdam]"), Pause.nextAt(now, 15, 30, zone))
    }

    /** AII-170: a picked day and time; a time already gone falls back to the next one. */
    @Test fun pickedDayAndTime() {
        val now = at("2026-03-10T15:30:00+01:00[Europe/Amsterdam]")
        assertEquals(at("2026-03-13T09:00:00+01:00[Europe/Amsterdam]"), Pause.onDay(now, java.time.LocalDate.of(2026, 3, 13), 9, 0, zone))
        assertEquals(at("2026-03-11T09:00:00+01:00[Europe/Amsterdam]"), Pause.onDay(now, java.time.LocalDate.of(2026, 3, 10), 9, 0, zone))
    }

    @Test fun tomorrowAcrossSummerTime() {
        assertEquals(at("2026-03-29T08:00:00+02:00[Europe/Amsterdam]"), Pause.untilTomorrow(at("2026-03-28T23:00:00+01:00[Europe/Amsterdam]"), zone))
    }
}
