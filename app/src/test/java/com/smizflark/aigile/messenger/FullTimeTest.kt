package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.fullTime
import com.smizflark.aigile.messenger.ui.sendTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/** AII-51: the exact time under a tapped bubble. */
class FullTimeTest {
    @Test fun fullDateAndTime() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.UK)
            assertEquals("Tue 13 Oct, 11:42", fullTime("2026-10-13T10:42:00Z", ZoneId.of("Europe/London")))
            Locale.setDefault(Locale.forLanguageTag("nl"))
            // Dutch month short forms may carry a dot ("okt."), depending on the phone's locale data.
            assertEquals("di 13 okt, 12:42", fullTime("2026-10-13T10:42:00Z", ZoneId.of("Europe/Amsterdam")).replace(".", ""))
        } finally { Locale.setDefault(old) }
    }

    /** AII-170: Sends at: just the time today, the weekday this week, the date after that. */
    @Test fun sendTimeSaysTheDayWhenNotToday() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.UK)
            val now = java.time.Instant.parse("2026-10-13T13:00:00Z"); val z = ZoneId.of("Europe/London")
            assertEquals("15:00", sendTime("2026-10-13T14:00:00Z", now, z))
            assertEquals("Fri 09:00", sendTime("2026-10-16T08:00:00Z", now, z))
            assertEquals("21 Oct, 09:00", sendTime("2026-10-21T08:00:00Z", now, z))
        } finally { Locale.setDefault(old) }
    }

    @Test fun noTimeNoText() = assertEquals("", fullTime("not a time"))
}
