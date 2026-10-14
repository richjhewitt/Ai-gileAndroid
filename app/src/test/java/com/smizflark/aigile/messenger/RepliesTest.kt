package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Replies
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-180: your own quick replies, one per line. */
class RepliesTest {
    @Test fun onePerLineInOrder() = assertEquals(listOf("Go", "Later"), Replies.parse("Go\nLater"))

    @Test fun blanksAndSpacesLeftOut() = assertEquals(listOf("Go", "Later"), Replies.parse("  Go \n\n   \nLater\r\n"))

    @Test fun repeatsOnce() = assertEquals(listOf("Go"), Replies.parse("Go\nGo"))

    @Test fun emptyMeansStarting() = assertEquals(emptyList<String>(), Replies.parse(" \n "))
}
