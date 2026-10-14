package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedPicturesTest {
    @Test fun formatsSizes() {
        assertEquals("0 B", SavedPictures.format(0))
        assertEquals("512 B", SavedPictures.format(512))
        assertEquals("1.5 KB", SavedPictures.format(1536))
        assertEquals("120 MB", SavedPictures.format(120L * 1024 * 1024))
        assertEquals("1.2 GB", SavedPictures.format(1288490189L))
        assertEquals("1023 KB", SavedPictures.format(1023L * 1024))
    }

    @Test fun keepsPicturesWaitingInTheOutbox() {
        val p = Pending(id = "1", chat = "c", text = "", target = null, at = "", files = listOf("/data/outbox/ab-cat.jpg"))
        val all = listOf("/data/outbox/ab-cat.jpg", "/cache/image_cache/x.0", "/cache/files/doc.pdf")
        assertEquals(listOf("/cache/image_cache/x.0", "/cache/files/doc.pdf"), SavedPictures.removable(all, listOf(p)))
        assertEquals(all, SavedPictures.removable(all, emptyList()))
    }
}
