package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.VoiceLock
import com.smizflark.aigile.messenger.data.VoiceLock.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-384: slide up to lock a voice note. */
class VoiceLockTest {
    @Test fun lockNeedsAboutEightyDp() {
        assertFalse(VoiceLock.reached(79f)); assertTrue(VoiceLock.reached(80f)); assertFalse(VoiceLock.reached(-100f))
    }
    @Test fun slidingUpLocksOnlyFromHold() {
        assertEquals(State.HOLD, VoiceLock.onMove(State.HOLD, 40f))
        assertEquals(State.LOCKED, VoiceLock.onMove(State.HOLD, 90f))
        assertEquals(State.LOCKED, VoiceLock.onMove(State.LOCKED, 0f))
        assertEquals(State.SENT, VoiceLock.onMove(State.SENT, 90f))
    }
    @Test fun letGoSendsUnlockedAndKeepsLocked() {
        assertEquals(State.SENT, VoiceLock.onRelease(State.HOLD, false))
        assertEquals(State.DELETED, VoiceLock.onRelease(State.HOLD, true))
        assertEquals(State.LOCKED, VoiceLock.onRelease(State.LOCKED, false))
    }
    @Test fun buttonsActOnlyWhenLocked() {
        assertEquals(State.SENT, VoiceLock.onSend(State.LOCKED))
        assertEquals(State.DELETED, VoiceLock.onDelete(State.LOCKED))
        assertEquals(State.HOLD, VoiceLock.onSend(State.HOLD))
        assertEquals(State.HOLD, VoiceLock.onDelete(State.HOLD))
    }
}
