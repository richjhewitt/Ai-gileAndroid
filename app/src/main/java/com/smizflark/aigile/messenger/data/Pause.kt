package com.smizflark.aigile.messenger.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Pause alerts for a while (AII-77). Pure, so it is unit tested: the clock is always passed in. */
object Pause {
    const val HOUR = 3_600_000L

    /** True while `until` (epoch millis, 0 = not paused) is still ahead. */
    fun paused(now: Long, until: Long): Boolean = until > now

    fun oneHour(now: Long): Long = now + HOUR
    fun fourHours(now: Long): Long = now + 4 * HOUR

    /** The next 08:00 in the given zone: this morning when pressed in the small hours, else tomorrow. */
    fun untilTomorrow(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long = nextAt(now, 8, 0, zone)

    /** Hour:minute on [day] (Send later, pick a day, AII-170); a time already gone means the next one. */
    fun onDay(now: Long, day: java.time.LocalDate, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val at = day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
        return if (at > now) at else nextAt(now, hour, minute, zone)
    }

    /** The next time the clock shows hour:minute: later today, else tomorrow (Send later, pick a time, AII-147). */
    fun nextAt(now: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
        return if (today > now) today
            else Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1).atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
    }
}
