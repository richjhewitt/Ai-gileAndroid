package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.Lock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockTest {
    @Test fun offNeverLocks() {
        assertFalse(Lock.shouldLock(false, false, null, 0))
        assertFalse(Lock.shouldLock(false, true, 0L, 10 * 60_000L))
    }

    @Test fun coldStartLocks() = assertTrue(Lock.shouldLock(true, false, null, 5_000))

    @Test fun backSoonStaysOpen() {
        assertFalse(Lock.shouldLock(true, true, 1_000, 1_000 + 60_000L, 60))
        assertFalse(Lock.shouldLock(true, true, null, 1_000))
    }

    @Test fun backAfterAMinuteLocks() = assertTrue(Lock.shouldLock(true, true, 1_000, 1_001 + 60_000L, 60))

    @Test fun straightAwayLocksOnAnyTimeAway() {
        assertTrue(Lock.shouldLock(true, true, 1_000, 1_001, 0))
        assertFalse(Lock.shouldLock(true, true, 1_000, 1_000, 0))
    }

    @Test fun fiveMinutesWaitsFiveMinutes() {
        assertFalse(Lock.shouldLock(true, true, 0, 300_000, 300))
        assertTrue(Lock.shouldLock(true, true, 0, 300_001, 300))
        assertFalse(Lock.shouldLock(true, true, 0, 120_000, 300))
    }

    @Test fun coldStartIgnoresTheWait() = assertTrue(Lock.shouldLock(true, false, null, 5_000, 300))
}
