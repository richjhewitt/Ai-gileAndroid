package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Gif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-188: the same GIF links as the board's gifLink. */
class GifTest {
    @Test fun gifAddresses() {
        assertEquals("https://example.com/cat.GIF", Gif.src("https://example.com/cat.GIF"))
        assertEquals("https://media2.giphy.com/media/abc/giphy.webp", Gif.src("https://media2.giphy.com/media/abc/giphy.webp"))
        assertEquals("https://i.giphy.com/media/abc/200.gif", Gif.src("https://i.giphy.com/media/abc/200.gif"))
        assertEquals("https://media.tenor.com/x/dance.gif", Gif.src("https://media.tenor.com/x/dance.gif"))
    }

    @Test fun giphyPageBecomesItsGif() {
        assertEquals("https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif", Gif.src("https://giphy.com/gifs/funny-cat-l0MYt5jPR6QX5pnqM"))
        assertEquals("https://media.giphy.com/media/xT9IgG50Fb7Mi0prBC/giphy.gif", Gif.src("https://www.giphy.com/gifs/xT9IgG50Fb7Mi0prBC/"))
    }

    @Test fun notGifs() {
        assertNull(Gif.src("http://example.com/cat.gif"))   // https only
        assertNull(Gif.src("https://example.com/cat.png"))
        assertNull(Gif.src("https://giphy.com/explore/cats"))
        assertNull(Gif.src("https://evil.com/media/x.webp"))
        assertNull(Gif.src("not a link"))
    }

    @Test fun inText() {
        val t = "Look https://example.com/a.gif. And (https://giphy.com/gifs/b-XyZ1) again https://example.com/a.gif"
        assertEquals(listOf("https://example.com/a.gif", "https://media.giphy.com/media/XyZ1/giphy.gif"), Gif.inText(t))
    }

    @Test fun codeDoesNotCount() {
        assertEquals(emptyList<String>(), Gif.inText("Run `curl https://example.com/a.gif`\n```\nhttps://example.com/b.gif\n```"))
    }

    @Test fun atMostThree() {
        assertEquals(3, Gif.inText((1..5).joinToString(" ") { "https://example.com/$it.gif" }).size)
    }

    @Test fun picsAreWeb() {
        val p = Gif.pics("https://example.com/a.gif").single()
        assert(p.web && p.image && !p.local)
    }
}
