package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkPreviewTest {
    private val url = "https://example.com/a/b"

    @Test fun readsOgTitleAndImage() {
        val p = LinkPreview.parse("""<html><head><title>Plain</title><meta property="og:title" content="Big &amp; Bold"><meta content='/pic.png' property='og:image'></head>""", url)!!
        assertEquals("Big & Bold", p.title)
        assertEquals("https://example.com/pic.png", p.image)
    }

    @Test fun fallsBackToTitleTag() {
        val p = LinkPreview.parse("<head><TITLE>\n  Hello   there </TITLE></head>", url)!!
        assertEquals("Hello there", p.title)
        assertNull(p.image)
    }

    @Test fun noTitleGivesNothing() = assertNull(LinkPreview.parse("<html><body>hi</body></html>", url))

    @Test fun onlyHttpsPictures() {
        val p = LinkPreview.parse("""<meta property="og:title" content="T"><meta property="og:image" content="http://example.com/p.png">""", url)!!
        assertNull(p.image)
    }

    @Test fun firstLinkOnlyAndNotGifs() {
        assertEquals("https://a.com/x", LinkPreview.linkIn("see https://a.com/x and https://b.com/y"))
        assertNull(LinkPreview.linkIn("plain http://a.com/x"))
        assertNull(LinkPreview.linkIn("look https://a.com/x https://media.giphy.com/media/abc/giphy.gif"))
        assertNull(LinkPreview.linkIn("no link"))
    }
}
