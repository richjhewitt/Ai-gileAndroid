package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EventGuessTest {
    private val z = ZoneId.of("Europe/Amsterdam")
    // Friday 16 Oct 2026, 10:00
    private val now = LocalDateTime.of(2026, 10, 16, 10, 0).atZone(z).toInstant().toEpochMilli()
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(z).toInstant().toEpochMilli()
    private fun g(s: String) = EventGuess.guess(s, now, z)

    @Test fun tomorrowAt3pm() = assertEquals(at(17, 15), g("Lunch tomorrow at 3pm"))
    @Test fun clock24Today() = assertEquals(at(16, 15), g("Call today 15:00"))
    @Test fun dutchMorgen() = assertEquals(at(17, 9), g("Tandarts morgen"))
    @Test fun dutchWeekday() = assertEquals(at(19, 20, 30), g("Eten op maandag 20:30"))
    @Test fun englishWeekdayHalf() = assertEquals(at(21, 17, 30), g("Meet Wednesday 5:30 pm"))
    @Test fun sameWeekdayIsNextWeek() = assertEquals(at(23, 9), g("Friday drinks"))
    @Test fun timeGoneRollsToTomorrow() = assertEquals(at(17, 8), g("Gym 8:00"))
    @Test fun noTime() = assertNull(g("Remember the milk"))
    @Test fun titleFirstLineMax80() {
        assertEquals("Hello", EventGuess.title("Hello\nsecond"))
        assertEquals(80, EventGuess.title("x".repeat(200)).length)
    }
}
