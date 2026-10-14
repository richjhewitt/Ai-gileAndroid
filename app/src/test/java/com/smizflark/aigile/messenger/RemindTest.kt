package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** AII-201: Remind me times, with a fixed clock and zone so the test never depends on today. */
class RemindTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private fun ms(h: Int, m: Int = 0, day: Int = 14) = ZonedDateTime.of(2026, 10, day, h, m, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun chatKeysAreTheChatsWithAReminder() {
        val r = { id: String, chat: String -> Reminder(id, chat, chat, id, "t", 1L) }
        assertEquals(setOf("a", "b"), Remind.chatKeys(listOf(r("1", "a"), r("2", "b"), r("3", "a"))))
        assertEquals(emptySet<String>(), Remind.chatKeys(emptyList()))
    }

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

    @Test fun aNoteGoesFirstAndOldRemindersStillLoad() {
        val plain = Reminder("a", "c", "C", "m", "text", 1L)
        assertEquals("text", Remind.withNote(plain))
        assertEquals("📝 call back\ntext", Remind.withNote(plain.copy(note = " call back ")))
        assertEquals("text", Remind.withNote(plain.copy(note = "  ")))
        assertNull(Remind.cleanNote("  "))
        assertEquals("x", Remind.cleanNote(" x "))
        val json = kotlinx.serialization.json.Json
        assertEquals(plain, json.decodeFromString<Reminder>("""{"id":"a","chat":"c","chatName":"C","msg":"m","text":"text","at":1}"""))
        assertEquals(plain.copy(note = "n"), json.decodeFromString<Reminder>(json.encodeToString(plain.copy(note = "n"))))
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

    @Test fun chatDetailsListsOnlyThisChatsRemindersSoonestFirst() {
        val a = Reminder("c|1", "c", "C", "1", "one", 300)
        val b = Reminder("c|2", "c", "C", "2", "two", 100)
        val o = Reminder("d|1", "d", "D", "1", "other", 50)
        assertEquals(listOf(b, a), Remind.inChat(listOf(a, o, b), "c"))
        assertEquals(emptyList<Reminder>(), Remind.inChat(listOf(o), "c"))
    }
}
