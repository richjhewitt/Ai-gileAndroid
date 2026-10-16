package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.Lock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockTest {
    @Test fun offNeverLocks() {
        assertFalse(Lock.shouldLock(false, false, null, 0))
        assertFalse(Lock.shouldLock(false, true, 0L, 10 * Lock.GRACE_MS))
    }

    @Test fun coldStartLocks() = assertTrue(Lock.shouldLock(true, false, null, 5_000))

    @Test fun backSoonStaysOpen() {
        assertFalse(Lock.shouldLock(true, true, 1_000, 1_000 + Lock.GRACE_MS))
        assertFalse(Lock.shouldLock(true, true, null, 1_000))
    }

    @Test fun backAfterAMinuteLocks() = assertTrue(Lock.shouldLock(true, true, 1_000, 1_001 + Lock.GRACE_MS))
}
