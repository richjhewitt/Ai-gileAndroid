package com.smizflark.aigile.messenger.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** AII-82: the app's clock. The phone's own clock, except in screenshot tests, which fix it so shots never change by themselves. */
object Now {
    var clock: Clock = Clock.systemDefaultZone()

    fun instant(): Instant = clock.instant()
    fun millis(): Long = clock.millis()
    fun today(): LocalDate = instant().atZone(ZoneId.systemDefault()).toLocalDate()
}
