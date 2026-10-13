package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.SwipeAction
import com.smizflark.aigile.messenger.data.swipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-61: right = read, left = mute toggle. */
class SwipeTest {
    @Test fun rightMarksRead() { assertEquals(SwipeAction.READ, swipeAction(true, false)); assertEquals(SwipeAction.READ, swipeAction(true, true)) }
    @Test fun leftMutes() = assertEquals(SwipeAction.MUTE, swipeAction(false, false))
    @Test fun leftOnMutedUnmutes() = assertEquals(SwipeAction.UNMUTE, swipeAction(false, true))
    @Test fun noDirectionDoesNothing() = assertNull(swipeAction(null, false))
}
