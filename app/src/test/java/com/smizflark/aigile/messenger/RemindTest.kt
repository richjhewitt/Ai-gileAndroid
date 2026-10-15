package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** AII-201: Remind me times, with a fixed clock and zone so the test never depends on today. */
class RemindTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private fun ms(h: Int, m: Int = 0, day: Int = 14) = ZonedDateTime.of(2026, 10, day, h, m, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun inAnHourIsSixtyMinutesOn() = assertEquals(ms(11, 30), Remind.at(ms(10, 30), Remind.When.HOUR, zone))

    @Test fun eveningIsSixPmToday() = assertEquals(ms(18), Remind.at(ms(10), Remind.When.EVENING, zone))

    @Test fun eveningGoneAfterSixPm() {
        assertNull(Remind.at(ms(18), Remind.When.EVENING, zone))
        assertNull(Remind.at(ms(21), Remind.When.EVENING, zone))
        assertEquals(listOf(Remind.When.HOUR, Remind.When.TOMORROW), Remind.choices(ms(21), zone))
    }

    @Test fun allThreeBeforeSixPm() =
        assertEquals(listOf(Remind.When.HOUR, Remind.When.EVENING, Remind.When.TOMORROW), Remind.choices(ms(9), zone))

    @Test fun tomorrowIsNineAm() {
        assertEquals(ms(9, day = 15), Remind.at(ms(10), Remind.When.TOMORROW, zone))
        assertEquals(ms(9, day = 15), Remind.at(ms(23, 59), Remind.When.TOMORROW, zone))
    }

    @Test fun dueThenLeft() {
        val a = Reminder("a", "c", "C", "m1", "one", 100)
        val b = Reminder("b", "c", "C", "m2", "two", 300)
        assertEquals(listOf(a), Remind.due(listOf(b, a), 200))
        assertEquals(listOf(b), Remind.left(listOf(b, a), 200))
    }

    @Test fun sameMessageReplacesItsReminder() {
        val a = Reminder("x", "c", "C", "m1", "one", 100)
        val a2 = a.copy(at = 500)
        assertEquals(listOf(a2), Remind.add(listOf(a), a2))
    }

    @Test fun cancelRemovesOnlyThatReminderAndListIsSoonestFirst() {
        val a = Reminder("a", "c", "C", "m1", "one", 300)
        val b = Reminder("b", "c", "C", "m2", "two", 100)
        assertEquals(listOf(b, a), Remind.sorted(listOf(a, b)))
        assertEquals(listOf(a), Remind.cancel(listOf(a, b), "b"))
        assertEquals("remind-c|m1", Remind.workName(Remind.idOf("c", "m1")))
    }

    /** AII-269: groups around midnight, soonest first, empty groups left out. */
    @Test fun groupsSplitAtMidnight() {
        fun r(id: String, at: Long) = Reminder(id, "c", "C", id, id, at)
        val now = ms(23, 59)
        val late = r("late", ms(23, 59, 14) + 60_000)   // 00:00 on the 15th
        val today = r("today", ms(23, 58))
        val tomorrowEnd = r("tend", ms(23, 59, 15))
        val later = r("later", ms(0, 0, 16))
        val g = Remind.groups(listOf(later, tomorrowEnd, late, today), now, zone)
        assertEquals(listOf(Remind.Group.TODAY, Remind.Group.TOMORROW, Remind.Group.LATER), g.map { it.first })
        assertEquals(listOf(today), g[0].second)
        assertEquals(listOf(late, tomorrowEnd), g[1].second)
        assertEquals(listOf(later), g[2].second)
    }

    @Test fun groupsLeaveOutEmptyOnes() {
        val a = Reminder("a", "c", "C", "m", "t", ms(15))
        assertEquals(listOf(Remind.Group.TODAY to listOf(a)), Remind.groups(listOf(a), ms(10), zone))
        assertEquals(emptyList<Any>(), Remind.groups(emptyList(), ms(10), zone))
    }
}
