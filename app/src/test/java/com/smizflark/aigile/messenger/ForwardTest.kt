package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Forward
import com.smizflark.aigile.messenger.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Forward a message to another chat (AII-71). */
class ForwardTest {
    private fun chat(name: String, sub: String = "") = ChatRef(Kind.FRIEND, name.lowercase(), name, sub, "🙂")

    @Test fun forwardedTextCarriesTheMarkFirst() =
        assertEquals("↪ Forwarded\n\nMenu is fixed", Forward.text("Menu is fixed"))

    @Test fun anEarlierMarkIsNotStacked() =
        assertEquals("↪ Forwarded\n\nhi", Forward.text("↪ Forwarded\n\nhi"))

    @Test fun splitFindsTheMark() {
        assertEquals(true to "hello\nthere", Forward.split("↪ Forwarded\n\nhello\nthere"))
        assertEquals(false to "plain", Forward.split("plain"))
        assertFalse(Forward.split("see ↪ Forwarded later").first)
    }

    @Test fun roundTrip() = assertEquals(true to "> **Lead:** a\n\nb", Forward.split(Forward.text("> **Lead:** a\n\nb")))

    @Test fun pickerShowsAllWhenSearchIsBlank() {
        val l = listOf(chat("Davy"), chat("Lead"))
        assertEquals(l, Forward.pickable(l, "  "))
    }

    @Test fun pickerSearchesNameAndSubtitleAnyCase() {
        val l = listOf(chat("Davy", "friend"), chat("Lead", "Scrum Master"), chat("Ann", "Davy's mum"))
        assertEquals(listOf("Davy", "Ann"), Forward.pickable(l, "dAVY").map { it.name })
        assertEquals(listOf("Lead"), Forward.pickable(l, "scrum").map { it.name })
        assertTrue(Forward.pickable(l, "zzz").isEmpty())
    }
}
