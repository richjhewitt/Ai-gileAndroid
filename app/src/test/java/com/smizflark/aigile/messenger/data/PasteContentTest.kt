package com.smizflark.aigile.messenger.data

import com.smizflark.aigile.messenger.data.PasteContent.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasteContentTest {
    @Test fun pictureTypes() {
        assertTrue(PasteContent.isPicture("image/gif"))
        assertTrue(PasteContent.isPicture("image/webp"))
        assertTrue(PasteContent.isPicture("IMAGE/PNG"))
        assertFalse(PasteContent.isPicture("text/plain"))
        assertFalse(PasteContent.isPicture(null))
    }

    @Test fun friendTakesPictures() {
        assertEquals(Verdict.ADD, PasteContent.verdict(Kind.FRIEND, listOf("image/gif")))
        assertEquals(Verdict.ADD, PasteContent.verdict(Kind.FRIEND, listOf("text/plain", "image/webp")))
    }

    @Test fun teamChatsRefuse() {
        assertEquals(Verdict.NOT_IN_TEAM, PasteContent.verdict(Kind.LEAD, listOf("image/gif")))
        assertEquals(Verdict.NOT_IN_TEAM, PasteContent.verdict(Kind.WATCHMAN, listOf("image/png")))
    }

    @Test fun plainTextIsIgnored() {
        assertEquals(Verdict.IGNORE, PasteContent.verdict(Kind.FRIEND, listOf("text/plain")))
        assertEquals(Verdict.IGNORE, PasteContent.verdict(Kind.LEAD, emptyList()))
    }
}
