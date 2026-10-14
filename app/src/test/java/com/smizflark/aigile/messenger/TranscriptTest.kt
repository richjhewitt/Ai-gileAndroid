package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Transcript
import org.junit.Assert.assertEquals
import org.junit.Test

/** Share a whole chat as text (AII-139). */
class TranscriptTest {
    private val ref = ChatRef(Kind.LEAD, "a", "AI-gile", "", "")

    @Test fun whoWhenWhatOldestFirst() {
        val c = Conversation(ref, listOf(
            Bubble("1", "Lead", "Ready?", "t1", false),
            Bubble("2", "", "Yes", "t2", true, pics = listOf(Pic("http://b/x/sea.png", "sea.png"))),
            Bubble("3", "", "Done", "t3", false),
        ))
        assertEquals("AI-gile\n\nLead, T1:\nReady?\n\nRich, T2:\n[sea.png]\nYes\n\nAI-gile, T3:\nDone\n",
            Transcript.of(c, "Rich") { it.uppercase() })
    }
}
