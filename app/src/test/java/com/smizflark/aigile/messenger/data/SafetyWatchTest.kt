package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-333: a friend's safety code that changes holds sending back until you accept the new words. */
class SafetyWatchTest {
    private fun friend(id: String, safety: String) = ChatRef(kind = Kind.FRIEND, id = id, name = id, subtitle = "", emoji = "", safety = safety)

    @Test fun firstWordsAreTakenAsTheyAre() {
        val known = SafetyWatch.learn(emptyMap(), listOf(friend("b", "apple bear"), friend("c", ""), ChatRef(Kind.LEAD, "x", "x", "", "")))
        assertEquals(mapOf("b" to "apple bear"), known)
        assertFalse(SafetyWatch.changed(known, "b", "apple bear"))
        assertFalse(SafetyWatch.changed(known, "new", "cat dog"))   // never seen: nothing to compare with
    }

    @Test fun newWordsAreAChangeUntilAccepted() {
        val known = mapOf("b" to "apple bear")
        assertTrue(SafetyWatch.changed(known, "b", "cat dog"))
        assertEquals(known, SafetyWatch.learn(known, listOf(friend("b", "cat dog"))))   // learning never swallows a change
        val ok = SafetyWatch.accept(known, "b", "cat dog")
        assertFalse(SafetyWatch.changed(ok, "b", "cat dog"))
    }

    @Test fun blankWordsAreNoChange() {
        val known = mapOf("b" to "apple bear")
        assertFalse(SafetyWatch.changed(known, "b", ""))   // the board sent none this time
        assertEquals(known, SafetyWatch.accept(known, "b", ""))
    }
}
