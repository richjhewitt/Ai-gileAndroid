package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.replyProgress
import org.junit.Assert.assertEquals
import org.junit.Test

/** Swipe a message right to reply (AII-101): only a full swipe counts. */
class SwipeReplyTest {
    @Test fun halfWayIsNotEnough() = assertEquals(0.5f, replyProgress(50f, 100f))

    @Test fun pastTheMarkCountsAsFull() = assertEquals(1f, replyProgress(130f, 100f))

    @Test fun noDragNoReply() = assertEquals(0f, replyProgress(0f, 100f))
}
