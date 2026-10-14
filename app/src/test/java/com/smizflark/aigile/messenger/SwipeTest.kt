package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.SwipeAction
import com.smizflark.aigile.messenger.data.SwipeChoice as C
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

    // AII-216

    @Test fun choicesFollowSettings() {
        assertEquals(SwipeAction.PIN, swipeAction(true, false, C.PIN, C.CLOSE))
        assertEquals(SwipeAction.CLOSE, swipeAction(false, false, C.PIN, C.CLOSE))
        assertEquals(SwipeAction.UNMUTE, swipeAction(true, true, C.MUTE, C.READ))
        assertEquals(SwipeAction.READ, swipeAction(false, true, C.MUTE, C.READ))
    }
    @Test fun nothingTurnsASwipeOff() { assertNull(swipeAction(true, false, C.NOTHING, C.MUTE)); assertNull(swipeAction(false, false, C.READ, C.NOTHING)) }
    @Test fun idsRoundTripAndUnknownFallsBack() {
        C.entries.forEach { assertEquals(it, C.of(it.id, C.NOTHING)) }
        assertEquals(C.READ, C.of("bogus", C.READ)); assertEquals(C.MUTE, C.of(null, C.MUTE))
    }
}
