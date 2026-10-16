package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Voice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-331: voice notes in friend chats. */
class VoiceTest {
    @Test fun lengthReadsMinutesAndSeconds() {
        assertEquals("0:00", Voice.label(0))
        assertEquals("0:07", Voice.label(7_400))
        assertEquals("1:00", Voice.label(60_000))
        assertEquals("2:00", Voice.label(Voice.MAX_MS))
        assertEquals("0:00", Voice.label(-5))
    }

    @Test fun underOneSecondIsDropped() {
        assertTrue(Voice.tooShort(999))
        assertFalse(Voice.tooShort(1_000))
    }

    @Test fun audioIsKnownByTypeOrName() {
        assertTrue(Voice.isAudio("audio/mp4"))
        assertTrue(Voice.isAudio("", "voice-1.m4a"))
        assertTrue(Voice.isAudio(Pic("/api/dms/contacts/c1/files/x", "voice-1.m4a", "")))
        assertFalse(Voice.isAudio("image/png", "a.png"))
        assertFalse(Pic("/x", "notes.txt", "text/plain").let { Voice.isAudio(it) })
    }

    @Test fun nameAndFileKeepTheSendName() {
        assertEquals("voice-1700.m4a", Voice.fileName(1700))
        assertEquals("voice-1700.m4a", Voice.recordingFile(1700).substringAfter('-'))   // what Repo.upload sends
        assertEquals("audio/mp4", Mapper.mimeOf(Voice.fileName(1700)))
    }
}
