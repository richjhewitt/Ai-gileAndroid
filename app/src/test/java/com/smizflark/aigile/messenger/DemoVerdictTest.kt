package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.DemoSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The demo has one card to test (AII-274); Accept and Send back change it on the phone only (AII-273). */
class DemoVerdictTest {
    @Test fun demoCardWaitsWithTestSteps() = runBlocking {
        val c = DemoSource().card("tunio", "TUN-7")!!
        assertEquals("verify", c.status)
        assertTrue(c.testSteps.lines().count { it.startsWith("- ") } in 2..3)
    }

    @Test fun acceptMovesItToDone() = runBlocking {
        val d = DemoSource()
        d.verdict("tunio", "TUN-7", accept = true)
        assertEquals("done", d.card("tunio", "TUN-7")!!.status)
    }

    @Test fun sendBackMovesItToTheTeam() = runBlocking {
        val d = DemoSource()
        d.verdict("tunio", "TUN-7", accept = false, text = "Empty file")
        assertEquals("team", d.card("tunio", "TUN-7")!!.status)
    }

    @Test fun otherCardsAreNotTest() = runBlocking {
        assertEquals("doing", DemoSource().card("ai-gile", "AIG-1")!!.status)
    }
}
