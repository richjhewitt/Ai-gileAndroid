package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Recent
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-155: recent searches, newest first, no repeats, at most 5. */
class RecentTest {
    @Test fun newestFirst() = assertEquals(listOf("pins", "stars"), Recent.add(listOf("stars"), "pins"))

    @Test fun aRepeatMovesUpInAnyCase() = assertEquals(listOf("Stars", "pins"), Recent.add(listOf("pins", "stars"), " Stars "))

    @Test fun tooShortIsLeftOut() = assertEquals(listOf("pins"), Recent.add(listOf("pins"), "a"))

    @Test fun keepsFive() = assertEquals(listOf("f", "a", "b", "c", "d").map { it + it }, Recent.add(listOf("aa", "bb", "cc", "dd", "ee"), "ff"))
}
