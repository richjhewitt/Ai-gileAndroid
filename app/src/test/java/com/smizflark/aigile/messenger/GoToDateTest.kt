package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.GoToDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-193: which day Go to date lands on. */
class GoToDateTest {
    private val days = listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9))

    @Test fun aDayWithMessagesIsPickedAsIs() = assertEquals(LocalDate.of(2026, 10, 5), GoToDate.target(days, LocalDate.of(2026, 10, 5)))
    @Test fun aQuietDayGoesToTheNextLaterOne() = assertEquals(LocalDate.of(2026, 10, 9), GoToDate.target(days, LocalDate.of(2026, 10, 6)))
    @Test fun nothingLaterMeansTheNewest() = assertNull(GoToDate.target(days, LocalDate.of(2026, 10, 10)))
    @Test fun anEarlierDayGoesToTheFirst() = assertEquals(LocalDate.of(2026, 10, 1), GoToDate.target(days, LocalDate.of(2026, 9, 1)))
    @Test fun rangeIsFirstToLastMessage() = assertEquals(days.first()..days.last(), GoToDate.range(days, LocalDate.of(2026, 10, 14)))
    @Test fun rangeNeverPassesToday() = assertEquals(days.first()..LocalDate.of(2026, 10, 7), GoToDate.range(days, LocalDate.of(2026, 10, 7)))
    @Test fun noMessagesNoRange() = assertNull(GoToDate.range(emptyList(), LocalDate.of(2026, 10, 7)))
    @Test fun pickerMillisAreUtcMidnight() {
        val d = LocalDate.of(2026, 3, 29)
        assertEquals(d, GoToDate.pickedDay(GoToDate.utcMillis(d)))
    }
}
