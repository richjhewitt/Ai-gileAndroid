package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Crash
import com.smizflark.aigile.messenger.data.Mapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-38: the bug report / crash log message sent to the team. */
class CrashTest {
    private val phone = "Pixel 8, Android 16"
    private val crash = "2026-10-13T02:00:00Z\n" + Crash.trace(IllegalStateException("boom"))

    @Test fun typedBugOnly() {
        val r = Crash.report("  Reply button does nothing  ", null, "0.1.0", phone)
        assertEquals("💬 Reply button does nothing", Mapper.quickTitle(r))
        assertTrue(r.contains("- App 0.1.0, Pixel 8, Android 16"))
        assertFalse(r.contains("```"))
    }

    @Test fun crashWithNoNoteGetsATitleAndTheTrace() {
        val r = Crash.report("", crash, "0.1.0", phone)
        assertEquals("💬 The app crashed", Mapper.quickTitle(r))
        assertTrue(r.contains("- Crashed at 2026-10-13T02:00:00Z"))
        assertTrue(r.contains("java.lang.IllegalStateException: boom"))
        assertTrue(r.trimEnd().endsWith("```"))
    }

    @Test fun longTraceIsCut() {
        val r = Crash.report("x", "2026-10-13T02:00:00Z\n" + "a".repeat(20000), "0.1.0", phone)
        assertTrue(r.length < 7000)
        assertTrue(r.contains("…"))
    }
}
