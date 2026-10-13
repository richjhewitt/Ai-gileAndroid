package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.work.isOffline
import com.smizflark.aigile.messenger.work.waitingToSend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Waiting messages send by themselves when signal returns (AII-55). */
class AutoSendTest {
    private fun p(id: String, failed: Boolean = false, offline: Boolean = false) =
        Pending(id, "lead:ai-gile", "hi", null, "2026-10-01T10:00:00Z", failed, offline = offline)

    @Test fun noConnectionIsOfflineButARefusalIsNot() {
        assertTrue(isOffline(BoardError("Can't reach the board.")))
        assertFalse(isOffline(BoardError("The board didn't accept this phone's key.", 401)))
        assertFalse(isOffline(IllegalStateException("x")))
    }

    @Test fun onlyNewAndOfflineOnesGoByThemselves() {
        val out = listOf(p("new"), p("offline", failed = true, offline = true), p("refused", failed = true))
        assertEquals(listOf("new", "offline"), waitingToSend(out).map { it.id })
    }
}
