package com.smizflark.aigile.messenger.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** AII-193: Go to date. [days] are the chat's local days that have messages (the same days the day rows show). */
object GoToDate {
    /** The day row to scroll to: the picked day, else the next later day with messages, else null (= the newest message). */
    fun target(days: Collection<LocalDate>, picked: LocalDate): LocalDate? = days.filter { it >= picked }.minOrNull()

    /** The date picker answers in UTC milliseconds of the picked day's midnight. */
    fun pickedDay(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun utcMillis(day: LocalDate): Long = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Days the picker allows: from the first to the last message (never past today by the app clock). */
    fun range(days: Collection<LocalDate>, today: LocalDate): ClosedRange<LocalDate>? {
        val first = days.minOrNull() ?: return null
        val last = (days.maxOrNull() ?: return null).let { if (it > today && first <= today) today else it }
        return first..last
    }
}
