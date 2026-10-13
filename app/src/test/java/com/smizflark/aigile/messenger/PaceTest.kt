package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Pace
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceTest {
    @Test fun slowsDownWhileQuiet() {
        val p = Pace(3_000, 30_000)
        val waits = List(8) { p.after(changed = false) }
        assertEquals(4_500L, waits.first())
        assertTrue(waits.zipWithNext().all { (a, b) -> b >= a })
        assertEquals(30_000L, waits.last())   // never slower than slow
    }

    @Test fun quickAgainWhenSomethingChanges() {
        val p = Pace(3_000, 30_000)
        repeat(10) { p.after(changed = false) }
        assertEquals(3_000L, p.after(changed = true))
    }

    @Test fun pokeWakesTheWaitAtOnce() = runBlocking {
        val p = Pace(3_000, 30_000)
        repeat(10) { p.after(changed = false) }
        p.poke()
        assertEquals(3_000L, p.wait)
        val t = System.currentTimeMillis()
        p.sleep()
        assertTrue(System.currentTimeMillis() - t < 1_000)
    }
}
