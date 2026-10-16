package com.smizflark.aigile.messenger.data

import android.content.Intent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** AII-394: Add to calendar. Guesses a start time from a simple day and/or time in a message (English or Dutch). */
object EventGuess {
    private val weekdays = mapOf(
        "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY, "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY, "sunday" to DayOfWeek.SUNDAY,
        "maandag" to DayOfWeek.MONDAY, "dinsdag" to DayOfWeek.TUESDAY, "woensdag" to DayOfWeek.WEDNESDAY, "donderdag" to DayOfWeek.THURSDAY,
        "vrijdag" to DayOfWeek.FRIDAY, "zaterdag" to DayOfWeek.SATURDAY, "zondag" to DayOfWeek.SUNDAY)
    private val ampm = Regex("""\b(\d{1,2})(?::(\d{2}))?\s?(am|pm)\b""")
    private val clock24 = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val wordRx = Regex("""[a-z]+""")

    /** Title: the first line, at most 80 characters. */
    fun title(text: String): String = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty().take(80)

    /** Start time in millis, or null when the text has no day or time. A day alone starts at 09:00; a time alone is today (tomorrow if gone). */
    fun guess(text: String, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val low = text.lowercase()
        val now = java.time.Instant.ofEpochMilli(nowMs).atZone(zone)
        val today = now.toLocalDate()
        val words = wordRx.findAll(low).map { it.value }.toList()
        val day: LocalDate? = when {
            words.any { it == "today" || it == "vandaag" } -> today
            words.any { it == "tomorrow" || it == "morgen" } -> today.plusDays(1)
            else -> words.firstNotNullOfOrNull { weekdays[it] }?.let { wd ->
                val ahead = (wd.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(if (ahead == 0) 7L else ahead.toLong())
            }
        }
        val time: LocalTime? = ampm.find(low)?.let { m ->
            val h = m.groupValues[1].toInt()
            if (h !in 1..12) null else LocalTime.of(h % 12 + if (m.groupValues[3] == "pm") 12 else 0, m.groupValues[2].ifEmpty { "0" }.toInt())
        } ?: clock24.find(low)?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        if (day == null && time == null) return null
        var start = (day ?: today).atTime(time ?: LocalTime.of(9, 0)).atZone(zone)
        if (day == null && start.isBefore(now)) start = start.plusDays(1)
        return start.toInstant().toEpochMilli()
    }

    /** The Add to calendar intent: title, full text, and a one-hour slot when a time was found. */
    fun intent(text: String, nowMs: Long): Intent {
        val i = Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)
            .putExtra(android.provider.CalendarContract.Events.TITLE, title(text))
            .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, text)
        guess(text, nowMs)?.let {
            i.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
            i.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, it + 3_600_000L)
        }
        return i
    }
}
