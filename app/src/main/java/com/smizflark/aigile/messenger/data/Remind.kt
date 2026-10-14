package com.smizflark.aigile.messenger.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

/** A message to be reminded about (AII-201): kept on this phone, with a copy of its text for the alert. */
@Serializable
data class Reminder(val id: String, val chat: String, val chatName: String, val msg: String, val text: String, val at: Long)

/** Remind me about a message (AII-201). Pure, so it is unit tested: the clock and zone are always passed in. */
object Remind {
    enum class When { HOUR, EVENING, TOMORROW }

    const val EVENING_HOUR = 18
    const val MORNING_HOUR = 9

    /** The choices to offer at [now]: this evening is hidden once 18:00 has passed. */
    fun choices(now: Long, zone: ZoneId = ZoneId.systemDefault()): List<When> =
        When.values().filter { it != When.EVENING || at(now, When.EVENING, zone) != null }

    /** When the reminder goes off (epoch millis), or null if that time has already passed (this evening after 18:00). */
    fun at(now: Long, w: When, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val t = Instant.ofEpochMilli(now).atZone(zone)
        return when (w) {
            When.HOUR -> now + Pause.HOUR
            When.EVENING -> t.toLocalDate().atTime(EVENING_HOUR, 0).atZone(zone).toInstant().toEpochMilli().takeIf { it > now }
            When.TOMORROW -> t.toLocalDate().plusDays(1).atTime(MORNING_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        }
    }

    /** Adds a reminder; a second one for the same message replaces the first. */
    fun add(all: List<Reminder>, r: Reminder) = all.filterNot { it.id == r.id } + r

    /** Reminders whose time has come at [now], soonest first. */
    fun due(all: List<Reminder>, now: Long) = all.filter { it.at <= now }.sortedBy { it.at }

    /** What is left once the due ones have been shown. */
    fun left(all: List<Reminder>, now: Long) = all.filter { it.at > now }

    fun idOf(chat: String, msg: String) = "$chat|$msg"
}
